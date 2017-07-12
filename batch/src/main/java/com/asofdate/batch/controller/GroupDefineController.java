package com.asofdate.batch.controller;

import com.asofdate.batch.dto.GroupDefineDto;
import com.asofdate.batch.entity.GroupDefineEntity;
import com.asofdate.batch.entity.GroupTaskEntity;
import com.asofdate.batch.entity.TaskArgumentEntity;
import com.asofdate.batch.entity.TaskDependencyEntity;
import com.asofdate.batch.service.GroupDefineService;
import com.asofdate.batch.service.GroupTaskService;
import com.asofdate.batch.service.TaskDependencyService;
import com.asofdate.hauth.authentication.JwtService;
import com.asofdate.utils.Hret;
import com.asofdate.utils.JoinCode;
import com.asofdate.utils.RetMsg;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by hzwy23 on 2017/6/1.
 */
@RestController
@RequestMapping(value = "/v1/dispatch/group/define")
public class GroupDefineController {
    private final Logger logger = LoggerFactory.getLogger(GroupDefineController.class);
    @Autowired
    private GroupDefineService groupDefineService;
    @Autowired
    private GroupTaskService groupTaskService;
    @Autowired
    private TaskDependencyService taskDependencyService;

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<GroupDefineEntity> getAll(HttpServletRequest request) {
        String domainID = JwtService.getConnUser(request).getDomainID();
        return groupDefineService.findAll(domainID);
    }

    /*
    * 新增任务组
    * */
    @RequestMapping(method = RequestMethod.POST)
    @ResponseBody
    public String add(HttpServletResponse response, HttpServletRequest request) {
        RetMsg retMsg = groupDefineService.add(parse(request));
        if (retMsg.checkCode()) {
            return Hret.success(retMsg);
        }
        response.setStatus(retMsg.getCode());
        return Hret.error(retMsg);
    }

    /*
    * 删除任务组
    * */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public String delete(HttpServletResponse response, HttpServletRequest request) {
        String json = request.getParameter("JSON");

        List<GroupDefineEntity> args = new GsonBuilder().create().fromJson(json,
                new TypeToken<List<GroupDefineEntity>>() {
                }.getType());
        RetMsg msg = groupDefineService.delete(args);
        if (msg.checkCode()) {
            return Hret.success(msg);
        }
        response.setStatus(msg.getCode());
        return Hret.error(msg);
    }


    /*
    * 更新任务组
    * */
    @RequestMapping(method = RequestMethod.PUT)
    @ResponseBody
    public String update(HttpServletResponse response, HttpServletRequest request) {
        RetMsg retMsg = groupDefineService.update(parse(request));
        if (!retMsg.checkCode()) {
            response.setStatus(retMsg.getCode());
            return Hret.error(retMsg);
        }
        return Hret.success(retMsg);
    }

    @RequestMapping(value = "/task", method = RequestMethod.GET)
    @ResponseBody
    public List<GroupTaskEntity> getTask(HttpServletRequest request) {
        String groupId = request.getParameter("group_id");
        return groupTaskService.getTask(groupId);
    }

    @RequestMapping(value = "/task/dependency", method = RequestMethod.GET)
    @ResponseBody
    public List<GroupTaskEntity> getTaskDependency(HttpServletRequest request) {
        String id = request.getParameter("id");
        return taskDependencyService.getTaskDependency(id);
    }

    /*
    * 查询任务组可以配置的上级依赖任务
    * 禁止将任务组现有的下级任务配置成他的上级任务组依赖
    * @param group_id 任务组编码
    * @return JSON 可以选择的任务组列表
    * */
    @RequestMapping(value = "/group/task/current", method = RequestMethod.GET)
    @ResponseBody
    public List<GroupTaskEntity> getGroupTasks(HttpServletRequest request) {
        String groupId = request.getParameter("group_id");
        String id = request.getParameter("id");
        logger.debug("group_id is ：{},id is:{}", groupId, id);
        return taskDependencyService.getGroupTask(groupId, id);
    }

    @RequestMapping(value = "/group/task/dependency", method = RequestMethod.POST)
    @ResponseBody
    public String addGroupTask(HttpServletResponse response, HttpServletRequest request) {
        String json = request.getParameter("JSON");
        List<TaskDependencyEntity> list = new GsonBuilder().create().fromJson(json,
                new TypeToken<List<TaskDependencyEntity>>() {
                }.getType());
        RetMsg retMsg = taskDependencyService.addTaskDependency(list);
        if (!retMsg.checkCode()) {
            response.setStatus(retMsg.getCode());
            return Hret.error(retMsg);
        }
        return Hret.success(retMsg);
    }

    @RequestMapping(value = "/group/task/dependency/delete", method = RequestMethod.POST)
    @ResponseBody
    public String deleteTaskDependency(HttpServletResponse response, HttpServletRequest request) {
        String uuid = request.getParameter("uuid");
        RetMsg retMsg = taskDependencyService.deleteTaskDependency(uuid);
        if (!retMsg.checkCode()) {
            response.setStatus(retMsg.getCode());
            return Hret.error(retMsg);
        }
        return Hret.success(retMsg);
    }

    @RequestMapping(value = "/task/argument", method = RequestMethod.GET)
    @ResponseBody
    public List<TaskArgumentEntity> getGroupTaskArgument(HttpServletRequest request) {
        String id = request.getParameter("id");
        return groupTaskService.getTaskArg(id);
    }

    @RequestMapping(value = "/task/argument/update", method = RequestMethod.POST)
    @ResponseBody
    public String updateArgValue(HttpServletResponse response, HttpServletRequest request) {
        String argValue = request.getParameter("arg_value");
        String uuid = request.getParameter("uuid");
        String argId = request.getParameter("arg_id");

        logger.info("uuid is:" + uuid + ",arg value is:" + argValue);
        RetMsg retMsg = groupDefineService.updateArg(argValue, uuid, argId);
        if (retMsg.checkCode()) {
            return Hret.success(retMsg);
        }
        response.setStatus(retMsg.getCode());
        return Hret.error(retMsg);
    }

    /*
    * 删除任务组中已经配置的任务信息
    * @param id 是任务组中,任务的唯一id
    * */
    @RequestMapping(value = "/task/delete", method = RequestMethod.POST)
    @ResponseBody
    public String deleteTask(HttpServletResponse response, HttpServletRequest request) {
        String id = request.getParameter("id");
        RetMsg retMsg = groupTaskService.deleteTask(id);
        if (!retMsg.checkCode()) {
            response.setStatus(retMsg.getCode());
            return Hret.error(retMsg);
        }
        return Hret.success(200, "success", null);
    }

    @RequestMapping(value = "/task/list/delete", method = RequestMethod.POST)
    @ResponseBody
    public String deleteTaskList(HttpServletResponse response, HttpServletRequest request) {
        String json = request.getParameter("JSON");
        Set<String> args = new HashSet<>();

        List<GroupDefineDto> list = new GsonBuilder().create().fromJson(json,
                new TypeToken<List<GroupDefineDto>>() {
                }.getType());

        for (GroupDefineDto m : list) {
            args.add(m.getId());
        }

        RetMsg retMsg = groupTaskService.deleteTask(args);
        if (retMsg.checkCode()) {
            return Hret.success(retMsg);
        }

        response.setStatus(retMsg.getCode());
        return Hret.error(retMsg);
    }

    @RequestMapping(value = "/task/add", method = RequestMethod.POST)
    @ResponseBody
    public String addTask(HttpServletResponse response, HttpServletRequest request) {
        String group_id = request.getParameter("group_id");
        String task_id = request.getParameter("task_id");
        String domain_id = request.getParameter("domain_id");
        String arg_list = request.getParameter("arg_list");
        String id = UUID.randomUUID().toString();

        RetMsg retMsg = groupTaskService.addTask(id, group_id, task_id, domain_id);
        if (!retMsg.checkCode()) {
            response.setStatus(retMsg.getCode());
            return Hret.error(retMsg);
        }

        if (!"[]".equals(arg_list)) {

            List<GroupDefineDto> list = new GsonBuilder().create().fromJson(arg_list,
                    new TypeToken<List<GroupDefineDto>>() {
                    }.getType());

            for (GroupDefineDto m : list) {
                m.setId(id);
                m.setDomainId(domain_id);
            }
            retMsg = groupTaskService.addGroupArg(list);
            if (!retMsg.checkCode()) {
                response.setStatus(retMsg.getCode());
                return Hret.error(retMsg);
            }
        }
        return Hret.success(retMsg);
    }

    private GroupDefineEntity parse(HttpServletRequest request) {
        String userId = JwtService.getConnUser(request).getUserId();
        GroupDefineEntity groupDefineEntity = new GroupDefineEntity();
        String groupId = JoinCode.join(request.getParameter("domain_id"), request.getParameter("group_id"));
        groupDefineEntity.setGroupId(groupId);
        groupDefineEntity.setCodeNumber(request.getParameter("group_id"));
        groupDefineEntity.setCreateUser(userId);
        groupDefineEntity.setModifyUser(userId);
        groupDefineEntity.setDomainId(request.getParameter("domain_id"));
        groupDefineEntity.setGroupDesc(request.getParameter("group_desc"));
        return groupDefineEntity;
    }
}