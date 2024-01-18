package org.dromara.resource.api;

import org.dromara.resource.api.domain.RemoteSms;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * 短信服务
 *
 * @author Feng
 */
public interface RemoteSmsService {

    /**
     * 同步方法：发送固定消息模板短信
     *
     * @param phone   目标手机号
     * @param message 短信内容
     * @return 封装了短信发送结果的 RemoteSms 对象
     */
    RemoteSms sendMessage(String phone, String message);

    /**
     * 同步方法：发送固定消息模板多模板参数短信
     *
     * @param phone    目标手机号
     * @param messages 短信模板参数，使用 LinkedHashMap 以保持参数顺序
     * @return 封装了短信发送结果的 RemoteSms 对象
     */
    RemoteSms sendMessage(String phone, LinkedHashMap<String, String> messages);

    /**
     * 同步方法：使用自定义模板发送短信
     *
     * @param phone      目标手机号
     * @param templateId 短信模板ID
     * @param messages   短信模板参数，使用 LinkedHashMap 以保持参数顺序
     * @return 封装了短信发送结果的 RemoteSms 对象
     */
    RemoteSms sendMessage(String phone, String templateId, LinkedHashMap<String, String> messages);

    /**
     * 同步方法：群发固定模板短信
     *
     * @param phones  目标手机号列表（1~1000）
     * @param message 短信内容
     * @return 封装了短信发送结果的 RemoteSms 对象
     */
    RemoteSms messageTexting(List<String> phones, String message);

    /**
     * 同步方法：使用自定义模板群发短信
     *
     * @param phones     目标手机号列表（1~1000）（1~1000）
     * @param templateId 短信模板ID
     * @param messages   短信模板参数，使用 LinkedHashMap 以保持参数顺序
     * @return 封装了短信发送结果的 RemoteSms 对象
     */
    RemoteSms messageTexting(List<String> phones, String templateId, LinkedHashMap<String, String> messages);

    /**
     * 异步方法：发送固定消息模板短信
     *
     * @param phone   目标手机号
     * @param message 短信内容
     */
    void sendMessageAsync(String phone, String message);

    /**
     * 异步方法：使用自定义模板发送短信
     *
     * @param phone      目标手机号
     * @param templateId 短信模板ID
     * @param messages   短信模板参数，使用 LinkedHashMap 以保持参数顺序
     */
    void sendMessageAsync(String phone, String templateId, LinkedHashMap<String, String> messages);

    /**
     * 延迟发送：发送固定消息模板短信
     *
     * @param phone       目标手机号
     * @param message     短信内容
     * @param delayedTime 延迟发送时间（毫秒）
     */
    void delayMessage(String phone, String message, Long delayedTime);

    /**
     * 延迟发送：使用自定义模板发送定时短信
     *
     * @param phone       目标手机号
     * @param templateId  短信模板ID
     * @param messages    短信模板参数，使用 LinkedHashMap 以保持参数顺序
     * @param delayedTime 延迟发送时间（毫秒）
     */
    void delayMessage(String phone, String templateId, LinkedHashMap<String, String> messages, Long delayedTime);

    /**
     * 延迟群发：群发延迟短信
     *
     * @param phones      目标手机号列表（1~1000）
     * @param message     短信内容
     * @param delayedTime 延迟发送时间（毫秒）
     */
    void delayMessageTexting(List<String> phones, String message, Long delayedTime);

    /**
     * 延迟群发：使用自定义模板发送群体延迟短信
     *
     * @param phones      目标手机号列表（1~1000）
     * @param templateId  短信模板ID
     * @param messages    短信模板参数，使用 LinkedHashMap 以保持参数顺序
     * @param delayedTime 延迟发送时间（毫秒）
     */
    void delayMessageTexting(List<String> phones, String templateId, LinkedHashMap<String, String> messages, Long delayedTime);

    /**
     * 加入黑名单
     *
     * @param phone 手机号
     */
    void addBlacklist(String phone);

    /**
     * 加入黑名单
     *
     * @param phones 手机号列表
     */
    void addBlacklist(List<String> phones);

    /**
     * 移除黑名单
     *
     * @param phone 手机号
     */
    void removeBlacklist(String phone);

    /**
     * 移除黑名单
     *
     * @param phones 手机号
     */
    void removeBlacklist(List<String> phones);

}
