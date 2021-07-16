package com.jdt.fedlearn.coordinator.service.task;

import com.jdt.fedlearn.coordinator.dao.db.TaskMapper;
import com.jdt.fedlearn.coordinator.entity.table.TaskAnswer;
import com.jdt.fedlearn.coordinator.entity.task.TaskListQuery;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TaskListImplTest {

    @BeforeClass
    public void init() {
        // a用户没有自己创建的任务，有两个加入的任务
        mockSelectNotOwnTask();
        mockSelectCreatedTask();
    }

    @Test
    public void testQueryTask() {
        TaskListImpl taskListImpl = new TaskListImpl();
        TaskListQuery taskListQuery = new TaskListQuery("a", "created", "mCode");
        List<TaskAnswer> list = taskListImpl.queryTask(taskListQuery);
        Assert.assertEquals(list.size(), 0);
        TaskListQuery taskListQuery2 = new TaskListQuery("a", "joined", "mCode");
        List<TaskAnswer> list2 = taskListImpl.queryTask(taskListQuery2);
        Assert.assertEquals(list2.size(), 2);
        TaskListQuery taskListQuery3 = new TaskListQuery("a", "option", "mCode");
        List<TaskAnswer> list3 = taskListImpl.queryTask(taskListQuery3);
        Assert.assertEquals(list3.size(), 0);


    }

    @Test
    public void testSelectJoinedTask() {
        TaskListImpl taskListImpl = new TaskListImpl();
        List<TaskAnswer> list = taskListImpl.selectJoinedTask("a");
        List<TaskAnswer> lines = TaskMapper.selectNotOwnTask("a");
        Assert.assertEquals(list.size(), lines.size());
        Assert.assertEquals(list.get(0).getHasPwd(), lines.get(0).getHasPwd());
        Assert.assertEquals(list.get(0).getParticipants(), lines.get(0).getParticipants());
        Assert.assertEquals(list.get(0).getTaskName(), lines.get(0).getTaskName());

    }

    @Test
    public void testSelectOptionTask() {
        TaskListImpl taskListImpl = new TaskListImpl();
        List<TaskAnswer> list = taskListImpl.selectOptionTask("d", "mCode");
        List<TaskAnswer> lines = TaskMapper.selectNotOwnTask("d");
        Assert.assertEquals(list.size(), lines.size());
        Assert.assertEquals(list.get(0).getHasPwd(), lines.get(0).getHasPwd());
        Assert.assertEquals(list.get(0).getParticipants(), lines.get(0).getParticipants());
        Assert.assertEquals(list.get(0).getTaskName(), lines.get(0).getTaskName());
    }

    @Test
    public void testParsePartners() {
        String[] res = TaskListImpl.parsePartners("[a,b,c]");
        Assert.assertEquals(res.length, 3);
    }

    private static void mockSelectNotOwnTask() {
        new MockUp<TaskMapper>() {
            @Mock
            public List<TaskAnswer> selectNotOwnTask(String username) {
                // 两个公开的任务
                TaskAnswer taskAnswer1 = new TaskAnswer(0, "taskName", "c", "[a,b,c]", "hasPwd", "merCode", "1", "[merCode]", "inferenceFlag");
                TaskAnswer taskAnswer2 = new TaskAnswer(1, "taskName", "c", "[a,b,c]", "hasPwd", "merCode", "1", "[merCode]", "inferenceFlag");
                List<TaskAnswer> list = new ArrayList<>();
                list.add(taskAnswer1);
                list.add(taskAnswer2);
                return list;
            }
        };
    }

    private static void mockSelectCreatedTask() {
        new MockUp<TaskMapper>() {
            @Mock
            public List<TaskAnswer> selectCreatedTask(String username) {
                // 两个公开的任务
                List<TaskAnswer> list = new ArrayList<>();
                return list;
            }
        };
    }

}