package org.dromara.common.log.event;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ip.AddressUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.RemoteClientService;
import org.dromara.system.api.RemoteLogService;
import org.dromara.system.api.domain.bo.RemoteLoginInfoBo;
import org.dromara.system.api.domain.bo.RemoteOperLogBo;
import org.dromara.system.api.domain.vo.RemoteClientVo;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 异步调用日志服务
 *
 * @author ruoyi
 */
@Component
@Slf4j
public class LogEventListener {

    @DubboReference
    private RemoteLogService remoteLogService;
    @DubboReference
    private RemoteClientService remoteClientService;

    /**
     * 保存系统日志记录
     */
    @EventListener
    public void saveLog(OperLogEvent operLogEvent) {
        RemoteOperLogBo sysOperLog = BeanUtil.toBean(operLogEvent, RemoteOperLogBo.class);
        remoteLogService.saveLog(sysOperLog);
    }

    /**
     * 保存系统访问记录
     */
    @EventListener
    public void saveLoginInfo(LoginInfoEvent loginInfoEvent) {
        HttpServletRequest request = ServletUtils.getRequest();
        final UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
        final String ip = ServletUtils.getClientIP(request);
        // 客户端信息
        String clientId = request.getHeader(LoginHelper.CLIENT_KEY);
        RemoteClientVo clientVo = null;
        if (StringUtils.isNotBlank(clientId)) {
            clientVo = remoteClientService.queryByClientId(clientId);
        }

        String address = AddressUtils.getRealAddressByIP(ip);
        String s = getBlock(ip) +
            address +
            getBlock(loginInfoEvent.getUsername()) +
            getBlock(loginInfoEvent.getStatus()) +
            getBlock(loginInfoEvent.getMessage());
        // 打印信息到日志
        log.info(s, loginInfoEvent.getArgs());
        // 封装对象
        RemoteLoginInfoBo loginInfo = new RemoteLoginInfoBo();
        loginInfo.setUserName(loginInfoEvent.getUsername());
        if (ObjectUtil.isNotNull(clientVo)) {
            loginInfo.setClientKey(clientVo.getClientKey());
            loginInfo.setDeviceType(clientVo.getDeviceType());
        }
        loginInfo.setIpaddr(ip);
        loginInfo.setLoginLocation(address);
        if (userAgent != null) {
            loginInfo.setBrowser(userAgent.getBrowser().getName());
            loginInfo.setOs(userAgent.getOs().getName());
        }
        loginInfo.setMsg(loginInfoEvent.getMessage());
        // 日志状态
        if (StringUtils.equalsAny(loginInfoEvent.getStatus(), Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER)) {
            loginInfo.setStatus(Constants.SUCCESS);
        } else if (Constants.LOGIN_FAIL.equals(loginInfoEvent.getStatus())) {
            loginInfo.setStatus(Constants.FAIL);
        }
        remoteLogService.saveLoginInfo(loginInfo);
    }

    private String getBlock(Object msg) {
        if (msg == null) {
            msg = "";
        }
        return "[" + msg + "]";
    }

}
