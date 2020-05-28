package com.shc.test;

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
        String datePattern = "yyyy-MM-dd hh:mm:ss";
        String datePat = "yyyy-MM-dd";
        SimpleDateFormat ft = new SimpleDateFormat (datePat);
        Date startTime = ft.parse("2017-08-29 11:20:18");
    }
}

