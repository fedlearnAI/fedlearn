package com.jdt.fedlearn.coordinator.entity.uniqueId;

import java.text.SimpleDateFormat;


/**
 *
 */
public interface UniqueId{
    String COMMON_DATE = "yyMMddHHmmss";
    ThreadLocal<SimpleDateFormat> df = ThreadLocal.withInitial(() -> new SimpleDateFormat(COMMON_DATE));
    String separator = "-";

}
