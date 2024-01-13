package org.dromara.common.core.utils;


import cn.hutool.core.convert.Convert;
import org.dromara.common.core.constant.RegexConstants;
import org.dromara.common.core.exception.ServiceException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则工具类
 *
 * @author Feng
 */
public class ReUtil {
    public final static Pattern GROUP_VAR = Pattern.compile("\\$(\\d+)");

    /**
     * 正则中需要被转义的关键字
     */
    public final static Set<Character> RE_KEYS = new HashSet<>(Arrays.asList('$', '(', ')', '*', '+', '.', '[', ']', '?', '\\', '^', '{', '}', '|'));

    /**
     * 判断字符串是否匹配指定的正则表达式
     *
     * @param input 要检查的字符串
     * @param regex 用于匹配的正则表达式，可以使用 {@link RegexConstants} 中定义的常量
     * @return 如果字符串与正则表达式匹配，返回 true；否则返回 false
     * @throws IllegalArgumentException 如果输入字符串或正则表达式为 null
     */
    public static boolean isValid(String input, String regex) {
        // 检查输入参数是否为null，如果是则抛出IllegalArgumentException
        if (StringUtils.isEmpty(input) || StringUtils.isEmpty(regex)) {
            throw new ServiceException("输入和正则表达式不得为空");
        }
        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);
        // 创建匹配器对象，并将输入字符串与正则表达式进行匹配
        Matcher matcher = pattern.matcher(input);
        // 返回匹配结果
        return matcher.matches();
    }

    /**
     * 从输入字符串中提取匹配的部分
     *
     * @param input 要提取的输入字符串
     * @param regex 用于匹配的正则表达式，可以使用 {@link RegexConstants} 中定义的常量
     * @return 如果找到匹配的部分，则返回匹配的部分，否则返回原始输入字符串
     */
    public static String extractFromString(String input, String regex) {
        // 检查输入参数是否为null，如果是则抛出IllegalArgumentException
        if (StringUtils.isEmpty(input) || StringUtils.isEmpty(regex)) {
            throw new ServiceException("输入和正则表达式不得为空");
        }
        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);
        // 创建匹配器
        Matcher matcher = pattern.matcher(input);
        // 查找匹配
        if (matcher.find()) {
            // 获取匹配的部分
            return matcher.group(1);
        } else {
            // 如果没有匹配，返回原始字符串
            return input;
        }
    }

    /**
     * 从输入字符串中提取匹配的部分，如果没有匹配则返回默认值
     *
     * @param input        要提取的输入字符串
     * @param regex        用于匹配的正则表达式，可以使用 {@link RegexConstants} 中定义的常量
     * @param defaultInput 如果没有匹配时返回的默认值
     * @return 如果找到匹配的部分，则返回匹配的部分，否则返回默认值
     */
    public static String extractFromString(String input, String regex, String defaultInput) {
        // 检查输入参数是否为null，如果是则抛出IllegalArgumentException
        if (StringUtils.isEmpty(input) || StringUtils.isEmpty(regex)) {
            throw new ServiceException("输入和正则表达式不得为空");
        }
        // 编译正则表达式
        Pattern pattern = Pattern.compile(regex);
        // 创建匹配器
        Matcher matcher = pattern.matcher(input);
        // 查找匹配
        if (matcher.find()) {
            // 获取匹配的部分
            return matcher.group(1);
        } else {
            // 如果没有匹配，返回默认值
            return defaultInput;
        }
    }

    /**
     * 正则替换指定值<br>
     * 通过正则查找到字符串，然后把匹配到的字符串加入到replacementTemplate中，$1表示分组1的字符串
     *
     * <p>
     * 例如：原字符串是：中文1234，我想把1234换成(1234)，则可以：
     *
     * <pre>
     * ReUtil.replaceAll("中文1234", "(\\d+)", "($1)"))
     *
     * 结果：中文(1234)
     * </pre>
     *
     * @param content             文本
     * @param regex               正则
     * @param replacementTemplate 替换的文本模板，可以使用$1类似的变量提取正则匹配出的内容
     * @return 处理后的文本
     */
    public static String replaceAll(CharSequence content, String regex, String replacementTemplate) {
        final Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        return replaceAll(content, pattern, replacementTemplate);
    }

    /**
     * 正则替换指定值<br>
     * 通过正则查找到字符串，然后把匹配到的字符串加入到replacementTemplate中，$1表示分组1的字符串
     *
     * @param content             文本
     * @param pattern             {@link Pattern}
     * @param replacementTemplate 替换的文本模板，可以使用$1类似的变量提取正则匹配出的内容
     * @return 处理后的文本
     * @since 3.0.4
     */
    public static String replaceAll(CharSequence content, Pattern pattern, String replacementTemplate) {
        if (StringUtils.isEmpty(content)) {
            return StringUtils.EMPTY;
        }

        final Matcher matcher = pattern.matcher(content);
        boolean result = matcher.find();
        if (result) {
            final Set<String> varNums = findAll(GROUP_VAR, replacementTemplate, 1, new HashSet<>());
            final StringBuffer sb = new StringBuffer();
            do {
                String replacement = replacementTemplate;
                for (String var : varNums) {
                    int group = Integer.parseInt(var);
                    replacement = replacement.replace("$" + var, matcher.group(group));
                }
                matcher.appendReplacement(sb, escape(replacement));
                result = matcher.find();
            }
            while (result);
            matcher.appendTail(sb);
            return sb.toString();
        }
        return Convert.toStr(content);
    }

    /**
     * 取得内容中匹配的所有结果
     *
     * @param <T>        集合类型
     * @param pattern    编译后的正则模式
     * @param content    被查找的内容
     * @param group      正则的分组
     * @param collection 返回的集合类型
     * @return 结果集
     */
    public static <T extends Collection<String>> T findAll(Pattern pattern, CharSequence content, int group,
                                                           T collection) {
        if (null == pattern || null == content) {
            return null;
        }

        if (null == collection) {
            throw new NullPointerException("Null collection param provided!");
        }

        final Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            collection.add(matcher.group(group));
        }
        return collection;
    }

    /**
     * 转义字符，将正则的关键字转义
     *
     * @param c 字符
     * @return 转义后的文本
     */
    public static String escape(char c) {
        final StringBuilder builder = new StringBuilder();
        if (RE_KEYS.contains(c)) {
            builder.append('\\');
        }
        builder.append(c);
        return builder.toString();
    }

    /**
     * 转义字符串，将正则的关键字转义
     *
     * @param content 文本
     * @return 转义后的文本
     */
    public static String escape(CharSequence content) {
        if (StringUtils.isBlank(content)) {
            return StringUtils.EMPTY;
        }

        final StringBuilder builder = new StringBuilder();
        int len = content.length();
        char current;
        for (int i = 0; i < len; i++) {
            current = content.charAt(i);
            if (RE_KEYS.contains(current)) {
                builder.append('\\');
            }
            builder.append(current);
        }
        return builder.toString();
    }

}
