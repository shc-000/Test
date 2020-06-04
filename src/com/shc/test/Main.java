package com.shc.test;

import sun.nio.ch.sctp.SendFailed;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    public static void main(String[] args) throws ParseException {
        //System.out.println(FullAttendActivityLabel.none.getLabel());
//        List<String> listA = new ArrayList<String>();
//        List<String> listB = new ArrayList<String>();
//        listA.add("A");
//        listA.add("B");
//        listB.add("A11");
//        listB.add("B");
//        listB.add("C");
//
//        List newList = new ArrayList();
//        newList.addAll(listA);
//
//        listA.retainAll(listB);
//        System.out.println(listA.size());
//        //System.out.println(listA.retainAll(listB));
//
//        newList.removeAll(listA);
//        System.out.println(newList);

//        getYesterdayRange();
        dateToStamp();

    }

    static void dateToStamp(){
        // 10位的秒级别的时间戳
        long time1 = 1591164945;
        String result1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time1 * 1000));
        System.out.println("10位数的时间戳（秒）--->Date:" + result1);
        Date date1 = new Date(time1*1000);   //对应的就是时间戳对应的Date
        // 13位的秒级别的时间戳
        double time2 = 1515730332000d;
        String result2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time2);
        System.out.println("13位数的时间戳（毫秒）--->Date:" + result2);
    }

    public static Map getYesterdayRange() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map condition=new HashMap();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.HOUR_OF_DAY,0);
        condition.put("endDate",df.format(calendar.getTime()));
        System.out.println("endDate is " + df.format(calendar.getTime()));
        calendar.set(Calendar.HOUR_OF_DAY,-24);
        condition.put("startDate",df.format(calendar.getTime()));
        System.out.println("startDate is " + df.format(calendar.getTime()));
        return condition;
    }
}

