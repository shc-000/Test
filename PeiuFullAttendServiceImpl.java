package com.voxlearning.xue.service.order.impl.service;

import com.voxlearning.alps.annotation.common.Mode;
import com.voxlearning.alps.annotation.remote.ExposeService;
import com.voxlearning.alps.annotation.remote.ImportService;
import com.voxlearning.alps.core.util.CollectionUtils;
import com.voxlearning.alps.lang.mapper.json.JsonUtils;
import com.voxlearning.alps.lang.util.MapMessage;
import com.voxlearning.alps.lang.util.SpringContainerSupport;
import com.voxlearning.alps.runtime.RuntimeMode;
import com.voxlearning.sentry.service.config.api.client.SentryCommonConfigLoaderClient;
import com.voxlearning.sentry.service.config.api.entity.SentryCommonConfig;
import com.voxlearning.xue.common.log.KibanaLogUtil;
import com.voxlearning.xue.service.coupon.api.entity.XueCoupon;
import com.voxlearning.xue.service.coupon.api.entity.XueCouponRef;
import com.voxlearning.xue.service.coupon.api.loader.XueCouponLoader;
import com.voxlearning.xue.service.coupon.api.service.XueCouponService;
import com.voxlearning.xue.service.course.api.CourseLoader;
import com.voxlearning.xue.service.course.api.LessonLoader;
import com.voxlearning.xue.service.course.entity.Course;
import com.voxlearning.xue.service.course.entity.SegmentLesson;
import com.voxlearning.xue.service.order.api.bean.*;
import com.voxlearning.xue.service.order.api.constans.*;
import com.voxlearning.xue.service.order.api.entity.PeiuFullAttendRewardRecord;
import com.voxlearning.xue.service.order.api.entity.XueUserOrder;
import com.voxlearning.xue.service.order.api.entity.XueUserRefundOrder;
import com.voxlearning.xue.service.order.api.service.PeiuFullAttendService;
import com.voxlearning.xue.service.order.api.service.XueUserOrderLoader;
import com.voxlearning.xue.service.order.api.service.XueUserRefundOrderLoader;
import com.voxlearning.xue.service.order.impl.dao.PeiuFullAttendRewardRecordDao;
import com.voxlearning.xue.service.user.api.loader.XueUserLoader;
import com.voxlearning.xue.service.user.entity.XueUserProfile;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.primitives.Longs.asList;

/**
 * 全勤奖励活动业务实现类
 *
 * @author haichao.shao
 * @since 2020-05-28 16:17
 */
@Named
@ExposeService(interfaceClass = PeiuFullAttendService.class)
@Slf4j
public class PeiuFullAttendServiceImpl extends SpringContainerSupport implements PeiuFullAttendService {

    @Inject
    private XueUserOrderLoader xueUserOrderLoader;
    @Inject
    private PeiuFullAttendRewardRecordDao peiuFullAttendRewardDao;
    @Inject
    private SentryCommonConfigLoaderClient sentryCommonConfigLoaderClient;

    @ImportService(interfaceClass = LessonLoader.class)
    private LessonLoader lessonLoader;

    @ImportService(interfaceClass = XueCouponLoader.class)
    private XueCouponLoader xueCouponLoader;

    @ImportService(interfaceClass = XueUserLoader.class)
    private XueUserLoader xueUserLoader;

    @ImportService(interfaceClass = XueCouponService.class)
    private XueCouponService xueCouponService;

    @ImportService(interfaceClass = CourseLoader.class)
    private CourseLoader courseLoader;

    @ImportService(interfaceClass = XueUserRefundOrderLoader.class)
    private XueUserRefundOrderLoader xueUserRefundOrderLoader;

    @Override
    public FullAttendStatusBO hasFullAttend(long courseId, long segmentId, String payOrderId) {
        XueUserOrder payOrder = xueUserOrderLoader.loadOrderById(payOrderId);

        if (!isPaid(payOrder)) {
            logger.info("The order is not paid, order is {}", payOrderId);
            return new FullAttendStatusBO(FullAttendActivityLabel.unpaid);
        }

        Map<Long, FullAttendRewardConfig> configMap = JsonUtils.fromJsonToMap(Objects.requireNonNull(getConfig(GeneralConfigKeyRecordConstants.PEIU_FULL_ATTEND_REWARD_CONFIG)).trim(), Long.class, FullAttendRewardConfig.class);
        if (null == configMap || !isJoined(configMap.keySet(), courseId)) {
            logger.warn("The course did not participate in discounts, courseId is {} ", courseId);
            return new FullAttendStatusBO(FullAttendActivityLabel.noActivity);
        }

        if (!isInTime(configMap.get(courseId))) {
            logger.info("The current time is not within the active time range, courseId is {}", courseId);
            return new FullAttendStatusBO(FullAttendActivityLabel.timeIsWrong);
        }

        PeiuFullAttendRewardRecord fullAttendRewardRecord = peiuFullAttendRewardDao.load(payOrderId);
        if (null != fullAttendRewardRecord) {
            logger.info("This order has already been applied for a discount, payOrderId is {}", payOrderId);
            if (FullAttendRewardType.refund.getCode() == fullAttendRewardRecord.getRewardTypeCode()) {
                return new FullAttendStatusBO(FullAttendActivityLabel.refunded);
            } else if (FullAttendRewardType.coupon.getCode() == fullAttendRewardRecord.getRewardTypeCode()) {
                return new FullAttendStatusBO(FullAttendActivityLabel.acquiredCoupons);
            } else {
                return new FullAttendStatusBO(FullAttendActivityLabel.stateError);
            }
        }

        if (!classIsEnd(segmentId)) {
            logger.info("The course is not over, segmentId is {}", segmentId);
            return new FullAttendStatusBO(FullAttendActivityLabel.courseNotEnd);
        }
        return new FullAttendStatusBO(FullAttendActivityLabel.fullAttend);
    }

    @Override
    public List<FullAttendInfoBO> getRewardInfo(String courseName, Double realPrice) {
        List<FullAttendInfoBO> listRewards = new ArrayList<>();

        FullAttendCouponConfig fullAttendCouponConfig = JsonUtils.fromJson(getConfig(GeneralConfigKeyRecordConstants.PEIU_FULL_ATTEND_COUPON), FullAttendCouponConfig.class);
        if (null == fullAttendCouponConfig) {
            logger.error("Configuration information does not exist！key is {}", GeneralConfigKeyRecordConstants.PEIU_FULL_ATTEND_COUPON);
            return null;
        }
        String name = fullAttendCouponConfig.getCouponCount() + "";
        listRewards.add(buildFullAttendInfoBO(name, fullAttendCouponConfig.getAmount(), fullAttendCouponConfig.getCouponName(), FullAttendRewardType.coupon.getCode()));

        listRewards.add(buildFullAttendInfoBO(courseName, realPrice, FullAttendRewardType.refund.getLabel(), FullAttendRewardType.refund.getCode()));

        return listRewards;
    }

    @Override
    public FullAttendStatusBO showReceivedReward(String payOrderId) {
        PeiuFullAttendRewardRecord fullAttendRewardRecord = peiuFullAttendRewardDao.load(payOrderId);
        if (null != fullAttendRewardRecord) {
            logger.info("This order has already been applied for a discount, payOrderId is {}", payOrderId);

            if (FullAttendRewardType.refund.getCode() == fullAttendRewardRecord.getRewardTypeCode()) {
                Double realPrice = fullAttendRewardRecord.getPrice();
                List<XueUserRefundOrder> refundOrders = xueUserRefundOrderLoader.loadRefundOrdersByOrderId(payOrderId);
                if (CollectionUtils.isEmpty(refundOrders)) {
                    //退款单ID还未生成
                    return new FullAttendStatusBO(FullAttendActivityLabel.refunded, realPrice);
                } else {
                    if (refundOrders.size() > 1) {//该场景下一个订单对应一个退款单
                        logger.error("This order corresponds to multiple refund orders, payOrderId is {}", payOrderId);
                        return new FullAttendStatusBO(FullAttendActivityLabel.stateError);
                    }
                    XueUserRefundOrder xueUserRefundOrder = refundOrders.get(0);
                    return new FullAttendStatusBO(parseStatus(xueUserRefundOrder.getStatus()), realPrice);
                }

            } else if (FullAttendRewardType.coupon.getCode() == fullAttendRewardRecord.getRewardTypeCode()) {
                List<Long> couponIds = fullAttendRewardRecord.getCouponIds();
                Map<Long, XueCoupon> xueCouponMap = xueCouponLoader.loadCouponByIds(couponIds);
                List<CouponInfoBO> couponInfoBOS = new ArrayList<>();
                for (XueCoupon coupon : xueCouponMap.values()) {
                    CouponInfoBO couponInfoBO = buildCouponInfoBO(coupon, FullAttendActivityLabel.applySuccess.getCode());
                    couponInfoBOS.add(couponInfoBO);
                }
                return new FullAttendStatusBO(FullAttendActivityLabel.acquiredCoupons, couponInfoBOS);
            } else {
                return new FullAttendStatusBO(FullAttendActivityLabel.stateError);
            }
        } else {
            return new FullAttendStatusBO(FullAttendActivityLabel.stateError);
        }
    }

    private FullAttendActivityLabel parseStatus(String label) {
        switch (XueUserRefundOrderStatus.valueOf(label)) {
            case Success:
                return FullAttendActivityLabel.refundSuccess;
            case Approve:
                return FullAttendActivityLabel.refundApprove;
            case Cancel:
                return FullAttendActivityLabel.refundCancel;
            case Apply:
                return FullAttendActivityLabel.applySuccess;
            default:
                return FullAttendActivityLabel.stateError;
        }
    }

    private FullAttendInfoBO buildFullAttendInfoBO(String name, Double price, String type, int typeCode) {
        return FullAttendInfoBO.builder()
                .name(name)
                .price(price)
                .type(type)
                .typeCode(typeCode)
                .build();
    }

    /**
     * 判断是否结课
     */
    private boolean classIsEnd(long courseSegmentId) {
        List<SegmentLesson> segmentLessons = lessonLoader.loadNotEndSegmentLessonBySegmentId(courseSegmentId);
        return CollectionUtils.isEmpty(segmentLessons);
    }

    /**
     * 判断是否支付
     */
    private boolean isPaid(XueUserOrder payOrder) {
        if (payOrder == null) {
            return false;
        }
        if (!Objects.equals(payOrder.getStatus(), XueUserOrderStatus.Paid.name())) {
            logger.info("current pay order status is wrong, order id: {}", payOrder.getId());
            return false;
        }
        return true;
    }

    /**
     * 判断是否当前时间是否处于活动时间内
     */
    private boolean isInTime(FullAttendRewardConfig peiuFullAttendConfig) {
        SimpleDateFormat ft = new SimpleDateFormat(GeneralConfigKeyRecordConstants.datePattern);

        Date startTime = null;
        Date endTime = null;
        try {
            startTime = ft.parse(peiuFullAttendConfig.getStartDateTime());
            endTime = ft.parse(peiuFullAttendConfig.getEndDateTime());
        } catch (ParseException e) {
            logger.error("Date format error, startDateTime is {}, endDateTime is {}", peiuFullAttendConfig.getStartDateTime(), peiuFullAttendConfig.getEndDateTime());
            e.printStackTrace();
        }
        Date nowTime = new Date();

        if (nowTime.getTime() == startTime.getTime()
                || nowTime.getTime() == endTime.getTime()) {
            return true;
        }

        Calendar date = Calendar.getInstance();
        date.setTime(nowTime);

        Calendar begin = Calendar.getInstance();
        begin.setTime(startTime);

        Calendar end = Calendar.getInstance();
        end.setTime(endTime);

        return (date.after(begin) && date.before(end));
    }

    /**
     * 读取配置文件
     */
    private String getConfig(String key) {
        SentryCommonConfig config = sentryCommonConfigLoaderClient.fetchSentryCommonConfig(key);
        if (null == config) {
            logger.error("No configuration file detected, key is {}", key);
            return null;
        }
        return config.getValue();
    }

    /**
     * 判断课程是否参与优惠活动
     */
    private boolean isJoined(Set<Long> courseIds, long courseId) {
        if (null == courseIds) return false;
        else return courseIds.contains(courseId);
    }

    /**
     * 判断是否全勤
     */
    private boolean isFullAttend(long studentId, long courseId) {

        Map<Long, FullAttendRewardConfig> configMap = JsonUtils.fromJsonToMap(getConfig(GeneralConfigKeyRecordConstants.PEIU_FULL_ATTEND_REWARD_CONFIG), Long.class, FullAttendRewardConfig.class);

        if (null == configMap) {
            //配置异常
            return false;
        }
        FullAttendRewardConfig fullAttendConfig = configMap.get(courseId);
        if (null == fullAttendConfig) {
            return false;
        }
        Set<Long> stuIds = fullAttendConfig.getFullAttendStuIds();
        if (CollectionUtils.isEmpty(stuIds)) {
            //该课程所有学生非全勤
            return false;
        }
        return stuIds.contains(studentId);
    }

    @Override
    public FullAttendStatusBO applyReward(long studentId, String payOrderId, long courseId, long segmentId, int rewardCode, String grade) {

        FullAttendStatusBO attendStatusBO = hasFullAttend(courseId, segmentId, payOrderId);
        if (FullAttendActivityLabel.fullAttend.getCode() != attendStatusBO.getCode()) {
            return attendStatusBO;
        }

        XueUserOrder payOrder = xueUserOrderLoader.loadOrderById(payOrderId);

        if (!isFullAttend(studentId, courseId)) {
            return new FullAttendStatusBO(FullAttendActivityLabel.notFullAttend);
        }

        FullAttendStatusBO fullAttendStatusBO = new FullAttendStatusBO();
        XueUserProfile xueUserProfile = xueUserLoader.loadXueUserProfile(studentId);
        String realName = xueUserProfile.getRealName();

        Course course = courseLoader.loadById(courseId).getUninterruptibly();
        String courseName = course.getName();

        Long realPrice = payOrder.getRealPrice();
        if (FullAttendRewardType.refund.getCode() == rewardCode) {
            fullAttendStatusBO = applyRefund(studentId, payOrderId, realPrice);
            if (FullAttendActivityLabel.applySuccess.getCode() == fullAttendStatusBO.getCode()) {
                saveKibanaLog(grade, courseName, courseId, realName, FullAttendRewardType.refund.name());
            }
        } else if (FullAttendRewardType.coupon.getCode() == rewardCode) {
            fullAttendStatusBO = applyCoupon(studentId, payOrderId);
            if (FullAttendActivityLabel.applySuccess.getCode() == fullAttendStatusBO.getCode()) {
                saveKibanaLog(grade, courseName, courseId, realName, FullAttendRewardType.coupon.name());
            }
        }
        return fullAttendStatusBO;
    }

    /**
     * 申请退款
     */
    private FullAttendStatusBO applyRefund(long studentId, String payOrderId, double realPrice) {

        if (null == saveData(studentId, payOrderId, realPrice, null, FullAttendRewardType.refund)) {
            logger.error("save full-attend-reward-refund has error:{}", studentId + "-" + payOrderId);
            return new FullAttendStatusBO(FullAttendActivityLabel.saveDataError);
        } else {
            return new FullAttendStatusBO(FullAttendActivityLabel.applySuccess);
        }
    }

    private PeiuFullAttendRewardRecord saveData(long studentId, String payOrderId, double price, List<Long> couponIds, FullAttendRewardType type) {
        PeiuFullAttendRewardRecord fullAttendRewardRecord = PeiuFullAttendRewardRecord.builder()
                .rewardTypeCode(type.getCode())
                .applyRewardType(type.getLabel())
                .applyDateTime(new Date())
                .price(price)
                .couponIds(couponIds)
                .studentId(studentId)
                .build();
        fullAttendRewardRecord.setId(payOrderId);
        return peiuFullAttendRewardDao.upsert(fullAttendRewardRecord);
    }

    /**
     * 申请优惠券
     */
    private FullAttendStatusBO applyCoupon(long studentId, String payOrderId) {

        FullAttendCouponConfig fullAttendCouponConfig = JsonUtils.fromJson(getConfig(GeneralConfigKeyRecordConstants.PEIU_FULL_ATTEND_COUPON), FullAttendCouponConfig.class);
        List<Long> couponIds = new ArrayList<>(fullAttendCouponConfig.getCouponIds());

        //bUsed:[true:已使用；false:未使用；null:全部]
        List<XueCouponRef> xueCouponRefs = xueCouponLoader.loadCouponRefByUserId(studentId, false).getUninterruptibly();//查询未使用的优惠券
        List<Long> ownedCouponIds = xueCouponRefs.stream().map(XueCouponRef::getCouponId).collect(Collectors.toList());
        //求交集，如果没有交集，则可以
        couponIds.retainAll(ownedCouponIds);

        Map<Long, String> map = new HashMap<>();
        if (couponIds.size() > 0) {//有交集（该情况，业务上说不存在，暂时保留代码）
            List<Long> addedCoupons = new ArrayList(fullAttendCouponConfig.getCouponIds());
            addedCoupons.removeAll(couponIds);
            if (0 == addedCoupons.size()) {
                //本次申请，实际领取优惠券0个,也要返回成功获取是3个优惠券
                logger.warn("The user already has all these coupons: {}", fullAttendCouponConfig.getCouponIds());
            } else {
                logger.warn("The user already has part of these coupons: {}", fullAttendCouponConfig.getCouponIds());
                map = grantCoupons(addedCoupons, studentId);
            }
        } else {//没有交集，全部添加到学生账户下
            logger.info("Give coupons {} to user {}", fullAttendCouponConfig.getCouponIds(), studentId);
            map = grantCoupons(fullAttendCouponConfig.getCouponIds(), studentId);//添加失败的
        }
        //只记录map中错误的为失败，如果上面存在交集情况，也视为成功。
        Map<Long, XueCoupon> xueCouponMap = xueCouponLoader.loadCouponByIds(fullAttendCouponConfig.getCouponIds());

        List<Long> successCouponIds;
        List<CouponInfoBO> couponInfoBOS = new ArrayList<>();
        if (null == map || 0 == map.size()) {//全部成功
            for (XueCoupon coupon : xueCouponMap.values()) {
                CouponInfoBO couponInfoBO = buildCouponInfoBO(coupon, FullAttendActivityLabel.applySuccess.getCode());
                couponInfoBOS.add(couponInfoBO);
            }
            successCouponIds = fullAttendCouponConfig.getCouponIds();
        } else {
            for (Long couponId : map.keySet()) {//失败的
                XueCoupon coupon = xueCouponMap.get(couponId);
                CouponInfoBO couponInfoBO = buildCouponInfoBO(coupon, FullAttendActivityLabel.applyError.getCode(), map.get(couponId));
                couponInfoBOS.add(couponInfoBO);
                xueCouponMap.remove(couponId);
            }

            //成功的
            for (XueCoupon coupon : xueCouponMap.values()) {
                CouponInfoBO couponInfoBO = buildCouponInfoBO(coupon, FullAttendActivityLabel.applySuccess.getCode());
                couponInfoBOS.add(couponInfoBO);
            }
            successCouponIds = new ArrayList<>(xueCouponMap.keySet());
        }

        if (null == saveData(studentId, payOrderId, 0, successCouponIds, FullAttendRewardType.coupon)) {
            logger.warn("save full-attend-reward-coupon has error:{}", studentId + "-" + payOrderId);
            return new FullAttendStatusBO(FullAttendActivityLabel.saveDataError);
        }
        //金额排序展示领取到的优惠券
        return new FullAttendStatusBO(FullAttendActivityLabel.applySuccess, couponInfoBOS.stream().sorted(Comparator.comparing(CouponInfoBO::getCouponPD).reversed()).collect(Collectors.toList()));
    }

    private CouponInfoBO buildCouponInfoBO(XueCoupon coupon, int code) {
        return buildCouponInfoBO(coupon, code, "success");
    }


    private CouponInfoBO buildCouponInfoBO(XueCoupon coupon, int code, String msg) {
        return CouponInfoBO.builder()
                .code(code)
                .couponPD(coupon.getCouponPD())
                .label(msg)
                .name(coupon.getName())
                .useStartDate(coupon.getUseStartDate())
                .useEndDate(coupon.getUseEndDate())
                .build();
    }

    /**
     * 返回发放失败的优惠券信息(用户已经存在的算发放成功)
     */
    private Map<Long, String> grantCoupons(List<Long> couponIds, Long userId) {
        Map<Long, String> map = new HashMap<>();

        for (Long couponId : couponIds) {
            MapMessage mapMessage = xueCouponService.grantCoupons(couponId, asList(userId), true);
            if (!mapMessage.isSuccess()) {
                map.put(couponId, mapMessage.getInfo());
            } else {
                Map<String, Set<Long>> resMap = (Map<String, Set<Long>>) mapMessage.get("data");
                if (null != resMap && resMap.size() > 0) {
                    Set<Long> failedSet = resMap.get("failed");
                    if (null != failedSet && failedSet.size() == 1) {
                        map.put(couponId, mapMessage.getInfo());
                    }
                }
            }
        }
        return map;
    }

    /**
     * 埋点
     */
    private void saveKibanaLog(String grade, String courseName, Long courseId, String userName, String type) {
        Map<String, String> map = new HashMap<>();
        map.put("method", "PeiuFullAttendServiceImpl.applyReward");
        map.put("grade", grade);
        map.put("courseName", courseName);
        map.put("courseId", courseId + "");
        map.put("userName", userName);
        map.put("type", type);
        map.put("op", "peiu_full_attend_reward_record");
        if (RuntimeMode.current().ge(Mode.PRODUCTION)) {
            KibanaLogUtil.log(map);
        }
    }
}
