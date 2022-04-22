package com.anhui.fabricbaasttp.request;


import com.anhui.fabricbaascommon.bean.Node;
import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@ApiModel(value = "Orderer添加请求")
public class ChannelAddOrdererRequest {
    @Pattern(regexp = ParamPattern.CHANNEL_NAME_REGEX, message = ParamPattern.CHANNEL_NAME_MSG)
    @ApiModelProperty(value = "通道名称", required = true)
    private String channelName;

    @NotNull
    @ApiModelProperty(value = "Orderer节点的信息（必须已经添加到网络）", required = true)
    private Node orderer;
}
