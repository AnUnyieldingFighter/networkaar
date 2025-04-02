package com.retrofits.net.common;

import android.text.TextUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.retrofits.utiles.RLog;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//把表情替换成\ud83c
public class BaseJsonReplace extends JsonSerializer<String> {
    //true 遇到表情就替换
    private boolean isAll;

    public BaseJsonReplace() {
    }

    public BaseJsonReplace(boolean isAll) {
        this.isAll = isAll;
    }


    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        int status = ((JsonWriteContext) gen.getOutputContext()).writeValue();
        RLog.e("serialize value->", value);
        switch (status) {
            case JsonWriteContext.STATUS_OK_AFTER_COLON:
                gen.writeRaw(':');
                break;
            case JsonWriteContext.STATUS_OK_AFTER_COMMA:
                gen.writeRaw(',');
                break;
            case JsonWriteContext.STATUS_EXPECT_NAME:
                gen.writeString(value);
                return;
        }

        //
        gen.writeRaw('"');
        value = onReplaceEmoji(value);
        char c = '\\';
        for (int i = 0; i < value.length(); i++) {
            char temp = value.charAt(i);
            if (temp == '"' && i == 0) {
                gen.writeRaw(c);
            }
            if (temp == '"' && i > 0 && value.charAt(i - 1) != c) {
                gen.writeRaw(c);
            }
            //换行符 10
            if (temp == '\n') {
                gen.writeRaw("\\n");
                continue;
            }
            if (temp == '\b') {
                gen.writeRaw("\\b");
                continue;
            }
            gen.writeRaw(temp);
        }
        //DLog.e("写入数据：" + value);
        gen.writeRaw('"');
    }

    /*@Override
    public void serialize(String value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        value = onReplaceEmoji(value);
        jsonGenerator.writeString(value);
    }*/
//D83C DD70-D83C DE51
    private Pattern pattern = Pattern.compile(
            "[\ud83c\udc00-\ud83c\udfff]|" +
                    "[\ud83d\udc00-\ud83d\udfff]|" +
                    "[\u2600-\u27ff]",
            Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

    //表情替换
    protected String onReplaceEmoji(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return msg;
        }
        String value = msg;
        //
        Matcher emojiMatcher = pattern.matcher(msg);
        while (emojiMatcher.find()) {
            String emoji = emojiMatcher.group(0);
            String emojiCode = toUnicode(emoji);
            //一个unicode码 可以表示的表情符，不用unicode码表示
            if (!isAll && emojiCode.length() < 7) {
                continue;
            }
            value = value.replace(emoji, emojiCode);
        }
        return value;
    }

    protected String toUnicode(String string) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            String v = Integer.toHexString(c);
            sb.append("\\u");
            sb.append(v.toUpperCase());
        }
        String v = sb.toString();
        return v;
    }
}
