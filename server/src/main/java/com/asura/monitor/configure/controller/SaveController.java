package com.asura.monitor.configure.controller;

import com.asura.framework.base.paging.SearchMap;
import com.google.gson.Gson;
import com.asura.common.controller.IndexController;
import com.asura.common.response.ResponseVo;
import com.asura.monitor.configure.conf.MonitorCacheConfig;
import com.asura.monitor.configure.entity.MonitorConfigureEntity;
import com.asura.monitor.configure.entity.MonitorContactGroupEntity;
import com.asura.monitor.configure.entity.MonitorContactsEntity;
import com.asura.monitor.configure.entity.MonitorGroupsEntity;
import com.asura.monitor.configure.entity.MonitorItemEntity;
import com.asura.monitor.configure.entity.MonitorMessageChannelEntity;
import com.asura.monitor.configure.entity.MonitorScriptsEntity;
import com.asura.monitor.configure.entity.MonitorTemplateEntity;
import com.asura.monitor.configure.service.MonitorConfigureService;
import com.asura.monitor.configure.service.MonitorContactGroupService;
import com.asura.monitor.configure.service.MonitorContactsService;
import com.asura.monitor.configure.service.MonitorGroupsService;
import com.asura.monitor.configure.service.MonitorItemService;
import com.asura.monitor.configure.service.MonitorMessageChannelService;
import com.asura.monitor.configure.service.MonitorScriptsService;
import com.asura.monitor.configure.service.MonitorTemplateService;
import com.asura.monitor.configure.thread.MakeCacheThread;
import com.asura.monitor.configure.util.ConfigureUtil;
import com.asura.util.CheckUtil;
import com.asura.util.DateUtil;
import com.asura.util.LdapAuthenticate;
import com.asura.util.PermissionsCheck;
import com.asura.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.List;

import static com.asura.monitor.configure.conf.MonitorCacheConfig.cacheGroupsKey;
import static com.asura.monitor.configure.conf.MonitorCacheConfig.cacheHostCnfigureKey;
import static com.asura.monitor.configure.conf.MonitorCacheConfig.cacheHostConfigKey;

/**
 * <p></p>
 * <p/>
 * <PRE>
 * <BR>
 * <BR>-----------------------------------------------
 * <BR>
 * </PRE>
 * 所有监控保存配置
 *
 * @author zhaozq14
 * @version 1.0
 * @date 2016/8/20 08:00:00
 */
@Controller
@RequestMapping("/monitor/configure/")
public class SaveController {

    @Autowired
    private MonitorItemService itemService;

    @Autowired
    private MonitorConfigureService configureService;

    @Autowired
    private MonitorGroupsService groupsService;

    @Autowired
    private MonitorScriptsService scriptsService;

    @Autowired
    private MonitorContactGroupService contactGroupService;

    @Autowired
    private MonitorContactsService contactsService;

    @Autowired
    private MonitorTemplateService templateService;

    @Autowired
    private IndexController indexController;

    @Autowired
    private PermissionsCheck permissionsCheck;

    private final Gson GSON = new Gson();
    private final RedisUtil REDIS_UTIL = new RedisUtil();

    @Autowired
    private LdapAuthenticate ldapAuthenticate;

    @Autowired
    private MonitorMessageChannelService channelService;

    @Autowired
    private CacheController cacheController;

    private final ConfigureUtil CONFIGURE_UTIL = new ConfigureUtil();

    /**
     * 模板列表
     *
     * @return
     */
    @RequestMapping("template/save")
    @ResponseBody
    public ResponseVo templateSave(MonitorTemplateEntity entity, HttpSession session) {
        String user = permissionsCheck.getLoginUser(session);
        entity.setLastModifyUser(user);
        entity.setLastModifyTime(DateUtil.getTimeStamp());
        if (entity.getTemplateId() != null) {
            templateService.update(entity);
        } else {
            templateService.save(entity);
        }
        REDIS_UTIL.set(MonitorCacheConfig.cacheTemplateKey + entity.getTemplateId(), GSON.toJson(entity));
        CONFIGURE_UTIL.updateHostUpdate("template");
        return ResponseVo.responseOk(null);
    }

    /**
     * 监控组配置
     *
     * @param entity
     * @param session
     *
     * @return
     */
    @RequestMapping("groups/save")
    @ResponseBody
    public ResponseVo groupsSave(MonitorGroupsEntity entity, HttpSession session) {
        String user = permissionsCheck.getLoginUser(session);
        entity.setLastModifyUser(user);
        entity.setLastModifyTime(DateUtil.getTimeStamp());
        if (entity.getGroupsId() != null) {
            groupsService.update(entity);
        } else {
            groupsService.save(entity);
        }
        REDIS_UTIL.set(cacheGroupsKey + entity.getGroupsId(), GSON.toJson(entity));
        CONFIGURE_UTIL.updateHostUpdate("groups");
        return ResponseVo.responseOk(null);
    }

    /**
     * 联系组配置删除
     *
     * @return
     */
    @RequestMapping("groups/deleteSave")
    @ResponseBody
    public ResponseVo groupsDelete(int id, HttpServletRequest request) {
        String user = permissionsCheck.getLoginUser(request.getSession());
        MonitorGroupsEntity entity = groupsService.findById(id, MonitorGroupsEntity.class);
        String lastUser = entity.getLastModifyUser();
        if (lastUser.equals(user) || user.equals("admin")) {
            indexController.logSave(request, "删除组" + GSON.toJson(entity));
            groupsService.delete(entity);
            REDIS_UTIL.del(MonitorCacheConfig.cacheGroupsKey.concat(id + ""));
        }
        return ResponseVo.responseOk(null);
    }

    /**
     * 联系人配置
     *
     * @param entity
     * @param request
     *
     * @return
     */
    @RequestMapping("contacts/save")
    @ResponseBody
    public ResponseVo contactsSave(MonitorContactsEntity entity, HttpServletRequest request) {
        String user = permissionsCheck.getLoginUser(request.getSession());
        entity.setLastModifyUser(user);
        entity.setLastModifyTime(DateUtil.getTimeStamp());
        if (entity.getContactsId() != null) {
            REDIS_UTIL.set(MonitorCacheConfig.cacheContactKey + entity.getContactsId(), GSON.toJson(entity));
            contactsService.update(entity);
        } else {
            contactsService.save(entity);
        }
        indexController.logSave(request, "添加监控联系人" + GSON.toJson(entity));
        cacheController.setContactCache();
        return ResponseVo.responseOk(null);
    }


    /**
     * 联系人配置删除
     *
     * @param id
     * @param request
     *
     * @return
     */
    @RequestMapping("contacts/deleteSave")
    @ResponseBody
    public ResponseVo contactsDelete(int id, HttpServletRequest request) {
        String user = permissionsCheck.getLoginUser(request.getSession());
        MonitorContactsEntity entity = contactsService.findById(id, MonitorContactsEntity.class);
        String lastUser = entity.getLastModifyUser();
        if (lastUser == null) {
            lastUser = "";
        }
        if (lastUser.equals(user) || user.equals("admin")) {
            indexController.logSave(request, "删除用户" + GSON.toJson(entity));
            contactsService.delete(entity);
            REDIS_UTIL.del(MonitorCacheConfig.cacheContactKey.concat(id + ""));
        }
        return ResponseVo.responseOk(null);
    }

    /**
     * 联系人组配置
     *
     * @param entity
     * @param request
     *
     * @return
     */
    @RequestMapping("contactGroup/save")
    @ResponseBody
    public ResponseVo contactGroupSave(MonitorContactGroupEntity entity, HttpServletRequest request) {
        String user = permissionsCheck.getLoginUser(request.getSession());
        entity.setLastModifyUser(user);
        entity.setLastModifyTime(DateUtil.getTimeStamp());
        if (entity.getGroupId() != null) {
            contactGroupService.update(entity);
        } else {
            entity.setStatus(1);
            contactGroupService.save(entity);
        }
        indexController.logSave(request, "添加监控联系组" + GSON.toJson(entity));
        cacheController.setContactGroupCache();
        return ResponseVo.responseOk(null);
    }

    /**
     *
     * @param hostId
     */
    public void initMonitor(String hostId){
        CONFIGURE_UTIL.initMonitor(hostId);
    }

    /**
     * 联系人组配置删除
     *
     * @param id
     * @param request
     *
     * @return
     */
    @RequestMapping("contactGroup/deleteSave")
    @ResponseBody
    public ResponseVo contactsGroupDelete(int id, HttpServletRequest request) {
        String user = permissionsCheck.getLoginUser(request.getSession());
        MonitorContactGroupEntity entity = contactGroupService.findById(id, MonitorContactGroupEntity.class);
        String lastUser = entity.getLastModifyUser();
        if (lastUser == null) {
            lastUser = "";
        }
        if (lastUser.equals(user) || user.equals("admin")) {
            indexController.logSave(request, "删除监控联系人组" + GSON.toJson(entity));
            contactGroupService.delete(entity);
            REDIS_UTIL.del(MonitorCacheConfig.cacheContactGroupKey.concat(id + ""));
        }
        return ResponseVo.responseOk(null);
    }

    /**
     * 脚本配置
     *
     * @param entity
     * @param request
     *
     * @return
     */
    @RequestMapping("script/save")
    @ResponseBody
    public ResponseVo scriptSave(MonitorScriptsEntity entity, HttpServletRequest request) {
        String user = permissionsCheck.getLoginUser(request.getSession());
        entity.setLastModifyUser(user);
        entity.setLastModifyTime(DateUtil.getTimeStamp());
        // 设置默认超时时间
        if (! CheckUtil.checkString(entity.getTimeOut()+"")){
            entity.setTimeOut(8);
        }
        if (entity.getScriptsId() != null) {
            scriptsService.update(entity);
        } else {
            List<MonitorScriptsEntity> r = scriptsService.getDataList(null, "selectMaxId");
            int id;
            try {
                id = r.get(0).getScriptsId() + 1;
            } catch (Exception e) {
                id = 1;
            }
            entity.setScriptsId(id);
            scriptsService.save(entity);
        }
        indexController.logSave(request, "添加脚本" + GSON.toJson(entity));
        REDIS_UTIL.setex(MonitorCacheConfig.cacheScriptIdKey + entity.getScriptsId(), 600, GSON.toJson(entity));
        CONFIGURE_UTIL.updateHostUpdate("script");
        CONFIGURE_UTIL.setHostMonitorQueue(itemService, configureService, entity.getScriptsId(), cacheController);
        return ResponseVo.responseOk(null);
    }


    /**
     * 项目配置
     *
     * @param entity
     * @param request
     *
     * @return
     */
    @RequestMapping("item/save")
    @ResponseBody
    public ResponseVo itemSave(MonitorItemEntity entity, HttpServletRequest request) {
        String user = permissionsCheck.getLoginUser(request.getSession());
        entity.setLastModifyUser(user);
        entity.setLastModifyTime(DateUtil.getTimeStamp());
        if (entity.getItemId() != null) {
            itemService.update(entity);
        } else {
            List<MonitorItemEntity> r = itemService.getDataList(null, "selectMaxId");
            int id;
            try {
                id = r.get(0).getItemId() + 1;
            } catch (Exception e) {
                id = 1;
            }
            entity.setItemId(id);
            itemService.save(entity);
        }
        indexController.logSave(request, "添加监控项目" + GSON.toJson(entity));
        cacheController.setItemCache();
        if (entity.getIsDefault() != null && entity.getIsDefault().equals("1")) {
            cacheController.setDefaultMonitorChange();
        }else{
            CONFIGURE_UTIL.setHostMonitorQueue(itemService, configureService, entity.getScriptId(), cacheController);
        }
        return ResponseVo.responseOk(null);
    }

    /**
     * 项目配置删除
     *
     * @param id
     * @param request
     *
     * @return
     */
    @RequestMapping("item/deleteSave")
    @ResponseBody
    public ResponseVo itemDelete(int id, HttpServletRequest request) {
        String user = permissionsCheck.getLoginUser(request.getSession());
        MonitorItemEntity entity = itemService.findById(id, MonitorItemEntity.class);
        String lastUser = entity.getLastModifyUser();
        SearchMap searchMap = new SearchMap();
        searchMap.put("itemId", id);
        List<MonitorConfigureEntity> configureEntities = configureService.getDataList(searchMap, "selectItemHosts");
        if (configureEntities.size() > 0){
            return ResponseVo.responseError("请删除有依赖的监控项目后重试, 还有" +configureEntities.size()+ "未删除");
        }

        if (lastUser.equals(user) || user.equals("admin")) {
            indexController.logSave(request, "删除监控项目" + GSON.toJson(entity));
            REDIS_UTIL.del(MonitorCacheConfig.cacheItemKey.concat(id + ""));
            // 如果删除默认配置, 就发送default设置
            if (entity.getIsDefault() != null && entity.getIsDefault().equals("1")) {
                cacheController.setDefaultMonitorChange();
            }else{
                CONFIGURE_UTIL.setHostMonitorQueue(itemService, configureService, entity.getScriptId(), cacheController);
            }
            itemService.delete(entity);
        }
        return ResponseVo.responseOk(null);
    }


    /**
     * 项目配置删除
     *
     * @param id
     * @param request
     *
     * @return
     */
    @RequestMapping("script/deleteSave")
    @ResponseBody
    public ResponseVo scriptDelete(int id, HttpServletRequest request) {
        String user = permissionsCheck.getLoginUser(request.getSession());
        MonitorScriptsEntity entity = scriptsService.findById(id, MonitorScriptsEntity.class);
        String lastUser = entity.getLastModifyUser();
        SearchMap searchMap = new SearchMap();
        searchMap.put("scriptsId", id);
        List<MonitorItemEntity> items = itemService.getDataList(searchMap, "selectItemScripts");
        if (items.size() > 0 ){
            return ResponseVo.responseError("请删除监控项目后再删除监控脚本");
        }
        if (lastUser.equals(user) || user.equals("admin")) {
            indexController.logSave(request, "删除监控脚本" + GSON.toJson(entity));
            REDIS_UTIL.del(MonitorCacheConfig.cacheScriptIdKey.concat(String.valueOf(id)));
            // 如果删除默认配置, 就发送default设置
            CONFIGURE_UTIL.setHostMonitorQueue(itemService, configureService, entity.getScriptsId(), cacheController);
            scriptsService.delete(entity);
        }
        return ResponseVo.responseOk(null);
    }

    /**
     * 消息通道保存
     *
     * @param entity
     * @param request
     *
     * @return
     */
    @RequestMapping("messages/save")
    @ResponseBody
    public ResponseVo messagesSave(MonitorMessageChannelEntity entity, HttpServletRequest request) {
        String user = permissionsCheck.getLoginUser(request.getSession());
        entity.setLastModifyUser(user);
        entity.setLastModifyTime(DateUtil.getTimeStamp());
        if (entity.getChannelId() != null) {
            channelService.update(entity);
        } else {
            channelService.save(entity);
        }
        indexController.logSave(request, "添加消息通道" + GSON.toJson(entity));
        REDIS_UTIL.set(MonitorCacheConfig.cacheChannelKey + entity.getChannelTp(), GSON.toJson(entity));
        return ResponseVo.responseOk(null);
    }

    /**
     * 监控配置保存
     *
     * @param entity
     * @param request
     *
     * @return
     */
    @RequestMapping("configure/save")
    @ResponseBody
    public ResponseVo configureSave(MonitorConfigureEntity entity, HttpServletRequest request) {
        String user = permissionsCheck.getLoginUser(request.getSession());
        entity.setLastModifyUser(user);
        entity.setLastModifyTime(DateUtil.getTimeStamp());
        String[] hosts;
        if (entity.getConfigureId() != null) {
            MonitorConfigureEntity configureEntity = configureService.findById(entity.getConfigureId(), MonitorConfigureEntity.class);
            hosts = configureEntity.getHosts().split(",");
            // 删除老的机器的监控配置
            CONFIGURE_UTIL.deleteOldConfigure(entity.getConfigureId() + "", hosts);
            configureService.update(entity);
        } else {
            List<MonitorConfigureEntity> r = configureService.getDataList(null, "selectMaxId");
            int id;
            try {
                id = r.get(0).getConfigureId() + 1;
            } catch (Exception e) {
                id = 1;
            }
            hosts = entity.getHosts().split(",");
            entity.setConfigureId(id);
            try {
                configureService.save(entity);
            }catch (Exception e){
                return ResponseVo.responseError("保存监控重复或数据库希尔错误 ".concat(e.toString()));
            }
        }
        REDIS_UTIL.set(MonitorCacheConfig.cacheConfigureKey + entity.getConfigureId(), GSON.toJson(entity));
        CONFIGURE_UTIL.makeHostMonitorTag(entity);
        CONFIGURE_UTIL.setUpdateMonitor(entity);
        MakeCacheThread cacheThread = new MakeCacheThread(cacheController, hosts);
        cacheThread.start();
        indexController.logSave(request, "添加监控" + GSON.toJson(entity));
        return ResponseVo.responseOk(null);
    }

    /**
     * 修改报警开关
     * @param value
     * @return
     */
    @RequestMapping("messages/recordSaveSetAlarm")
    @ResponseBody
    public ResponseVo recordSaveSetAlarm(String value, HttpServletRequest request) {
        indexController.logSave(request, "修改报警开关 " + value);
        REDIS_UTIL.set(MonitorCacheConfig.cacheAlarmSwitch, value);
        return ResponseVo.responseOk("ok");
    }

    /**
     * 删除监控
     *
     * @return
     */
    @RequestMapping("configure/delete")
    @ResponseBody
    public String deleteConfigure(int id, HttpServletRequest request) {
        String user = permissionsCheck.getLoginUser(request.getSession());
        if (user.length() < 2) {
            return "请登陆后操作";
        }
        String dept = ldapAuthenticate.getSignUserInfo("department", "sAMAccountName=" + user);
        MonitorConfigureEntity configureEntity = configureService.findById(id, MonitorConfigureEntity.class);
        String lastModifyUser = configureEntity.getLastModifyUser();
        if (!user.equals(lastModifyUser) && !user.equals("admin") && !dept.contains("运维")) {
            return "no permissions";
        }
        String hosts = configureEntity.getHosts();
        String[] hostsList = hosts.split(",");
        for (String hostId : hostsList) {
            if (hostId.equals("") || hostId.length() < 1) {
                continue;
            }
            String delConfigKey = cacheHostCnfigureKey + hostId + "_" + id;
            REDIS_UTIL.del(delConfigKey);
            CONFIGURE_UTIL.initMonitor(hostId);
            String key = MonitorCacheConfig.cacheDefaultChangeQueue + hostId;
            REDIS_UTIL.del(key);
            REDIS_UTIL.lpush(key, "1");
            String configKey = cacheHostConfigKey + hostId;
            String configs = REDIS_UTIL.get(configKey);
            if (configs != null && configs.length() > 0) {
                HashSet<String> configData = new HashSet<>();
                HashSet<String> configSet = GSON.fromJson(configs, HashSet.class);
                for (String confKey : configSet) {
                    if (!confKey.trim().equals(id + "".trim())) {
                        configData.add(confKey);
                    }
                }
                REDIS_UTIL.set(configKey, GSON.toJson(configData));
            }

        }

        configureService.delete(configureEntity);
        REDIS_UTIL.del(MonitorCacheConfig.cacheConfigureKey + id);
        indexController.logSave(request, "删除监控配置" + GSON.toJson(configureEntity));
        return "ok";
    }
}