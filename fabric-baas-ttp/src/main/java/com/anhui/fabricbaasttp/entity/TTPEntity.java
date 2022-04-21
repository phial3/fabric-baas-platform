package com.anhui.fabricbaasttp.entity;


import com.anhui.fabricbaascommon.constant.ParamPattern;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Pattern;

@Document(collection = "ttp")
@ApiModel("可信第三方实体")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TTPEntity {
    @Pattern(regexp = ParamPattern.ORG_NAME_REGEX, message = ParamPattern.ORG_NAME_MSG)
    @ApiModelProperty(value = "可信第三方组织名称", required = true)
    @Id
    private String name;

    @Pattern(regexp = ParamPattern.HOST_REGEX, message = ParamPattern.HOST_MSG)
    @ApiModelProperty(value = "可信第三方组织域名", required = true)
    private String domain;

    @Pattern(regexp = ParamPattern.COUNTRY_CODE_REGEX, message = ParamPattern.COUNTRY_CODE_MSG)
    @ApiModelProperty(value = "可信第三方组织国家代码（例如CN、US）", required = true)
    private String countryCode;

    @Pattern(regexp = ParamPattern.STATE_OR_PROVINCE_REGEX, message = ParamPattern.STATE_OR_PROVINCE_MSG)
    @ApiModelProperty(value = "可信第三方所在省份，例如Guangxi", required = true)
    private String stateOrProvince;

    @Pattern(regexp = ParamPattern.LOCALITY_REGEX, message = ParamPattern.LOCALITY_MSG)
    @ApiModelProperty(value = "可信第三方所在城市，例如Guilin", required = true)
    private String locality;
}