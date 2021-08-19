package com.jdt.fedlearn.common.entity.uniqueId;


import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.type.MappingType;

import java.text.ParseException;
import java.util.Date;

public class MappingId implements UniqueId, Message {
    private final String projectId;
    private final MappingType mappingType;
    private final Date createTime;

    public MappingId(String projectId, MappingType mappingType) {
        this.projectId = projectId;
        this.mappingType = mappingType;
        this.createTime = new Date();

    }

    public MappingId(String trainId) throws ParseException{
        String[] parseRes = trainId.split(separator);
        this.projectId = parseRes[0];
        this.mappingType = MappingType.valueOf(parseRes[1]);
        this.createTime = df.get().parse(parseRes[2]) ;
    }

    private String generate() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.projectId);
        sb.append(separator);
        sb.append(this.mappingType.getType());
        sb.append(separator);
        sb.append(df.get().format(this.createTime));
        return sb.toString();
    }

    public String getMappingId(){
        return generate();
    }

    public String getProjectId() {
        return projectId;
    }

    public MappingType getMappingType() {
        return mappingType;
    }

    public Date getCreateTime() {
        return createTime;
    }

}
