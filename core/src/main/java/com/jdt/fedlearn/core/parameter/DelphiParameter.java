package com.jdt.fedlearn.core.parameter;

import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.parameter.common.CategoryParameter;
import com.jdt.fedlearn.core.parameter.common.MultiParameter;
import com.jdt.fedlearn.core.parameter.common.NumberParameter;
import com.jdt.fedlearn.core.parameter.common.ParameterField;
import com.jdt.fedlearn.core.type.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DelphiParameter implements SuperParameter{
    public DelphiParameter() {
    }

    @Override
    public String serialize() {
        return this.toString();
    }

    @Override
    public List<ParameterField> obtainPara() {
        List<ParameterField> res = new ArrayList<>();
        return res;
    }

    @Override
    public MetricType[] fetchMetric() {
        return new MetricType[0];
    }

    @Override
    public String toString() {
        return "TreeInferenceParameter{}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
//        TreeInferenceParameter parameter = (TreeInferenceParameter) o;
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash();
        result = 31 * result;
        return result;
    }

}




