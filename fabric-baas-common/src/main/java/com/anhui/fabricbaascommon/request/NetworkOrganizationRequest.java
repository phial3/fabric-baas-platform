package com.anhui.fabricbaascommon.request;

import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel(value = "操作网络中的组织请求")
public class NetworkOrganizationRequest extends BaseNetworkRequest {
    @NotNull
    @Pattern(regexp = ParamPattern.ORGANIZATION_NAME_REGEX, message = ParamPattern.ORGANIZATION_NAME_MSG)
    @ApiModelProperty(value = "组织名称", required = true)
    private String organizationName;
}
