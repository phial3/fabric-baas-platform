package com.anhui.fabricbaasorg.request;

import com.anhui.fabricbaascommon.request.BaseChannelRequest;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "提交加入通道邀请请求")
public class ChannelSubmitInvitationCodesRequest extends BaseChannelRequest {
    @NotEmpty
    @ApiModelProperty(value = "通道内所有组织的邀请信息（如果当前组织已经有其他节点加入过通道则允许为空）", required = false)
    private List<String> invitationCodes;
}
