package io.rackshift.dhcpproxy.model;

import com.alibaba.fastjson.JSONObject;
import io.rackshift.dhcpproxy.mapper.BareMetalMapper;
import io.rackshift.dhcpproxy.mapper.TaskMapper;
import io.rackshift.dhcpproxy.util.MySqlUtil;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;

@Data
public class BareMetal {
    private String id;
    private String pxeMac;
    private String status;

    public static BareMetal findByMac(String pxeMac) {
        SqlSession sqlSession = MySqlUtil.getConn();
        BareMetalMapper bareMetalMapper = sqlSession.getMapper(BareMetalMapper.class);
        BareMetal bareMetal = bareMetalMapper.findByMac(pxeMac);
        sqlSession.close();
        return bareMetal;
    }

    public boolean discovered() {
        if (!this.status.equalsIgnoreCase("onrack") && !this.status.equalsIgnoreCase("discovering")) {
            return true;
        }

        return false;
    }

    public boolean isRunningTask() {
        SqlSession sqlSession = MySqlUtil.getConn();
        TaskMapper taskMapper = sqlSession.getMapper(TaskMapper.class);
        boolean r = StringUtils.isNotBlank(taskMapper.selectActiveGraphObjectsByBMId(this.id));
        sqlSession.close();
        return r;
    }

    public boolean isRequestProfile() {
        SqlSession sqlSession = MySqlUtil.getConn();
        TaskMapper taskMapper = sqlSession.getMapper(TaskMapper.class);
        String graphObjectStr = taskMapper.selectActiveGraphObjectsByBMId(this.id);
        sqlSession.close();
        if (StringUtils.isNotBlank(graphObjectStr)) {
            JSONObject tasks = JSONObject.parseObject(graphObjectStr);
            boolean shouldProxy = false;
            for (String s : tasks.keySet()) {
                JSONObject task = tasks.getJSONObject(s);
                if (task.containsKey("options")) {
                    if (task.getJSONObject("options").containsKey("profile") && !StringUtils.equals(task.getString("state"), "succeeded")) {
                        shouldProxy = true;
                        break;
                    }
                }
            }
            return shouldProxy;
        }

        return false;
    }
}
