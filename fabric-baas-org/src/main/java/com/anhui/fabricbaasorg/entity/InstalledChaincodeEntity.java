package com.anhui.fabricbaasorg.entity;

import com.anhui.fabricbaascommon.bean.InstalledChaincode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Document(collection = "installedchaincode")
@AllArgsConstructor
@NoArgsConstructor
@Data
@ApiModel(value = "已安装的链码信息")
public class InstalledChaincodeEntity extends InstalledChaincode {
    @ApiModelProperty(value = "链码所在的Peer", required = true)
    private String peerName;
}
