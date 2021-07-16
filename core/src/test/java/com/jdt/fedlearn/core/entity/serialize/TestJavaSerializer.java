package com.jdt.fedlearn.core.entity.serialize;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import org.testng.annotations.Test;

public class TestJavaSerializer {
    @Test
    public void serialize(){
        Serializer serializer = new JsonSerializer();
        InferenceInit init = new InferenceInit(new String[]{"a", "b"});
        String str = serializer.serialize(init);
        System.out.println(str);
        System.out.println(str);
        Message body = serializer.deserialize(str);
    }

    @Test
    public void serializeArray(){
        int[] intArray = new int[]{1,2,3,4,5};
//        String str = SerializeUtil.serializeToString(intArray);
    }

    //测试入口
//    public static void main(String[] args) throws Exception {
//        Student student = new Student();
//        student.setId("1");
//        student.setName("王老三");
//        String serialStr = serializeToString(student);
//        System.out.println(serialStr);
//        Student deserialStudent = (Student) deserializeToObject(serialStr);
//        System.out.println(deserialStudent.getName());//输出王老三
//    }
}
