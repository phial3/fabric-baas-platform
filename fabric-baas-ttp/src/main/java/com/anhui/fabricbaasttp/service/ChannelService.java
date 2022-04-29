package com.anhui.fabricbaasttp.service;

import com.anhui.fabricbaascommon.bean.*;
import com.anhui.fabricbaascommon.constant.CertfileType;
import com.anhui.fabricbaascommon.exception.*;
import com.anhui.fabricbaascommon.fabric.ChannelUtils;
import com.anhui.fabricbaascommon.fabric.ConfigtxUtils;
import com.anhui.fabricbaascommon.response.ResourceResult;
import com.anhui.fabricbaascommon.service.CAService;
import com.anhui.fabricbaascommon.service.MinIOService;
import com.anhui.fabricbaascommon.util.CertfileUtils;
import com.anhui.fabricbaascommon.util.RandomUtils;
import com.anhui.fabricbaascommon.util.ResourceUtils;
import com.anhui.fabricbaascommon.util.ZipUtils;
import com.anhui.fabricbaasttp.bean.Orderer;
import com.anhui.fabricbaasttp.bean.Peer;
import com.anhui.fabricbaasttp.constant.MinIOBucket;
import com.anhui.fabricbaasttp.entity.ChannelEntity;
import com.anhui.fabricbaasttp.entity.NetworkEntity;
import com.anhui.fabricbaasttp.repository.ChannelRepo;
import com.anhui.fabricbaasttp.request.*;
import com.anhui.fabricbaasttp.response.ChannelQueryOrdererResult;
import com.anhui.fabricbaasttp.response.ChannelQueryPeerResult;
import com.anhui.fabricbaasttp.response.InvitationCodeResult;
import com.anhui.fabricbaasttp.util.IdentifierGenerator;
import com.anhui.fabricbaascommon.util.InvitationUtils;
import com.anhui.fabricbaasweb.util.SecurityUtils;
import com.spotify.docker.client.exceptions.NodeNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * TODO: 对传入通道名称不能为系统通道名称
 * TODO: 让不同网络里面的通道可以重名
 */
@Service
@Slf4j
public class ChannelService {
    @Autowired
    private NetworkService networkService;
    @Autowired
    private ChannelRepo channelRepo;
    @Autowired
    private MinIOService minioService;
    @Autowired
    private CAService caService;
    @Autowired
    private FabricEnvService fabricEnvService;

    private void assertOrganizationInChannel(ChannelEntity channel, String orgName) throws OrganizationException {
        if (!channel.getOrganizationNames().contains(orgName)) {
            throw new OrganizationException("当前组织不是通道的合法成员");
        }
    }

    private ChannelEntity getChannelOrThrowException(String channelName) throws ChannelException {
        Optional<ChannelEntity> channelOptional = channelRepo.findById(channelName);
        if (channelOptional.isEmpty()) {
            throw new ChannelException("未找到相应名称的通道");
        }
        return channelOptional.get();
    }

    public String getPeerIdentifierOrThrowException(ChannelEntity channel, Node peer) throws NodeException {
        List<Peer> peers = channel.getPeers();
        for (int i = 0, size = peers.size(); i < size; i++) {
            Peer endpoint = peers.get(i);
            if (endpoint.getHost().equals(peer.getHost()) &&
                    endpoint.getPort() == peer.getPort()) {
                return IdentifierGenerator.ofPeer(channel.getName(), i);
            }
        }
        throw new NodeException("未找到相应的Peer");
    }

    private static Node findNodeByAddress(Node node, List<? extends Node> nodes) {
        String target = node.getAddr();
        for (Node item : nodes) {
            if (item.getAddr().equals(target)) {
                return node;
            }
        }
        return null;
    }

    private void signEnvelopeWithOrganizations(String networkName, List<String> organizationNames, File envelope) throws IOException, InterruptedException, EnvelopeException {
        for (String orgName : organizationNames) {
            MSPEnv organizationMspDir = fabricEnvService.buildMSPEnvForOrg(networkName, orgName);
            log.info(String.format("正在使用%s在%s网络的证书对Envelope进行签名：", orgName, networkName) + envelope.getAbsolutePath());
            ChannelUtils.signEnvelope(organizationMspDir, envelope);
        }
    }

    private void assertInvitationCodes(List<String> invitationCodes, List<String> invitorOrgNames, String inviteeOrgName, String channelName) throws Exception {
        Set<String> actualInvitorSet = new HashSet<>();
        Set<String> givenInvitorSet = new HashSet<>(invitorOrgNames);
        for (String invitationCode : invitationCodes) {
            Invitation invitation = InvitationUtils.parseCode(invitationCode);
            if (!invitation.getChannelName().equals(channelName)) {
                throw new InvitationException("邀请码所包含的通道名称错误");
            }
            if (!invitation.getInviteeOrgName().equals(inviteeOrgName)) {
                throw new InvitationException("邀请码所包含的受邀组织错误");
            }
            actualInvitorSet.add(invitation.getInvitorOrgName());
        }
        if (!actualInvitorSet.equals(givenInvitorSet)) {
            throw new InvitationException("必须包含所有通道中的组织的邀请码");
        }
    }

    public ResourceResult createChannel(ChannelCreateRequest request) throws Exception {
        // 检查操作的组织是否属于相应的网络
        String curOrgName = SecurityUtils.getUsername();
        NetworkEntity network = networkService.getNetworkOrThrowException(request.getNetworkName());
        networkService.assertOrganizationInNetwork(network, curOrgName);

        // 检查相同名称的通道是否已经存在
        Optional<ChannelEntity> channelOptional = channelRepo.findById(request.getChannelName());
        if (channelOptional.isPresent()) {
            throw new ChannelException("相同名称的通道已存在");
        }

        // 默认配置该网络中当前的所有Orderer作为排序节点
        String organizationCertfileId = IdentifierGenerator.ofCertfile(request.getNetworkName(), curOrgName);
        File organizationCertfileDir = CertfileUtils.getCertfileDir(organizationCertfileId, CertfileType.ADMIN);
        ConfigtxOrganization ordererConfigtxOrg = new ConfigtxOrganization(
                caService.getAdminOrganizationName(),
                caService.getAdminOrganizationName(),
                new File(caService.getAdminCertfileDir() + "/msp")
        );
        log.info("生成Orderer组织的配置：" + ordererConfigtxOrg);

        ConfigtxOrganization currentConfigtxOrg = new ConfigtxOrganization(curOrgName, curOrgName, new File(organizationCertfileDir + "/msp"));
        List<ConfigtxOrganization> configtxOrganizations = Collections.singletonList(currentConfigtxOrg);
        log.info("生成当前组织的配置：" + currentConfigtxOrg);

        List<ConfigtxOrderer> configtxOrderers = new ArrayList<>(network.getOrderers().size());
        for (Orderer endpoint : network.getOrderers()) {
            File tlsServerCrt = new File(CertfileUtils.getCertfileDir(endpoint.getCaUsername(), CertfileType.ORDERER) + "/tls/server.crt");

            ConfigtxOrderer configtxOrderer = new ConfigtxOrderer();
            configtxOrderer.setHost(endpoint.getHost());
            configtxOrderer.setPort(endpoint.getPort());
            configtxOrderer.setServerTlsCert(tlsServerCrt);
            configtxOrderer.setClientTlsCert(tlsServerCrt);
            configtxOrderers.add(configtxOrderer);
        }
        log.info("生成通道的Orderer节点配置：" + configtxOrderers);

        // 生成configtx.yaml配置文件
        File configtxDir = ResourceUtils.createTempDir();
        File configtxYaml = new File(configtxDir + "/configtx.yaml");
        ConfigtxUtils.generateConfigtx(configtxYaml, network.getConsortiumName(), configtxOrderers, ordererConfigtxOrg, configtxOrganizations);
        log.info(String.format("生成基本configtx.yaml文件%s：", configtxYaml) + FileUtils.readFileToString(configtxYaml, StandardCharsets.UTF_8));
        ConfigtxUtils.appendChannelToConfigtx(configtxYaml, request.getChannelName(), Collections.singletonList(curOrgName));
        log.info(String.format("将通道配置加入configtx.yaml文件%s：", configtxYaml) + FileUtils.readFileToString(configtxYaml, StandardCharsets.UTF_8));

        // 随机选择一个Orderer
        Orderer orderer = RandomUtils.select(network.getOrderers());
        log.info("随机从网络中选择Orderer：" + orderer);

        // 创建通道
        MSPEnv organizationMspEnv = fabricEnvService.buildMSPEnvForOrg(network.getName(), curOrgName);
        TLSEnv ordererTlsEnv = fabricEnvService.buildTLSEnvForOrderer(orderer);
        log.info("生成组织的MSP环境变量：" + organizationMspEnv);
        log.info("生成Orderer的TLS环境变量：" + ordererTlsEnv);
        File appChannelGenesis = ResourceUtils.createTempFile("block");
        ChannelUtils.createChannel(organizationMspEnv, ordererTlsEnv, configtxDir, request.getChannelName(), appChannelGenesis);

        // 将通道创世区块保存至MinIO
        String channelId = IdentifierGenerator.ofChannel(request.getNetworkName(), request.getChannelName());
        minioService.putFile(MinIOBucket.APP_CHANNEL_GENESIS_BLOCK_BUCKET_NAME, channelId, appChannelGenesis);
        log.info("将创世区块保存到MinIO：" + appChannelGenesis.getAbsolutePath());

        // 将通道信息保存至MongoDB
        ChannelEntity channel = new ChannelEntity();
        channel.setName(request.getChannelName());
        channel.setNetworkName(request.getNetworkName());
        channel.setOrganizationNames(Collections.singletonList(curOrgName));
        channel.setPeers(Collections.emptyList());
        channel.setOrderers(network.getOrderers());
        channelRepo.save(channel);
        log.info("保存通道信息：" + channel);

        String downloadUrl = String.format("/download/block/%s.block", UUID.randomUUID());
        FileUtils.copyFile(appChannelGenesis, new File("static" + downloadUrl));
        ResourceResult result = new ResourceResult();
        result.setDownloadUrl(downloadUrl);
        return result;
    }

    public ResourceResult queryGenesisBlock(ChannelQueryGenesisBlockRequest request) throws Exception {
        ChannelEntity channel = getChannelOrThrowException(request.getChannelName());
        String curOrgName = SecurityUtils.getUsername();
        assertOrganizationInChannel(channel, curOrgName);
        Orderer orderer = RandomUtils.select(channel.getOrderers());

        String downloadUrl = "/download/block/" + UUID.randomUUID() + ".block";
        File block = new File("static" + downloadUrl);

        CoreEnv ordererCoreEnv = fabricEnvService.buildCoreEnvForOrderer(orderer);
        ChannelUtils.fetchGenesisBlock(ordererCoreEnv, request.getChannelName(), block);

        ResourceResult result = new ResourceResult();
        result.setDownloadUrl(downloadUrl);
        return result;
    }

    public void addOrderer(ChannelAddOrdererRequest request) throws Exception {
        // 检查通道是否存在
        ChannelEntity channel = getChannelOrThrowException(request.getChannelName());
        NetworkEntity network = networkService.getNetworkOrThrowException(channel.getNetworkName());

        // 检查当前组织是否位于通道中
        String curOrgName = SecurityUtils.getUsername();
        assertOrganizationInChannel(channel, curOrgName);

        // 保证Orderer已经在网络中
        Orderer newOrderer = (Orderer) findNodeByAddress(request.getOrderer(), network.getOrderers());
        if (newOrderer == null) {
            throw new NodeNotFoundException("Orderer不在网络中");
        }
        if (findNodeByAddress(request.getOrderer(), channel.getOrderers()) != null) {
            throw new NodeException("Orderer已经加入通道中");
        }

        // 从现有的通道中随机选择一个Orderer节点
        Orderer selectedOrderer = RandomUtils.select(channel.getOrderers());
        File selectedOrdererCertfileDir = CertfileUtils.getCertfileDir(selectedOrderer.getCaUsername(), CertfileType.ORDERER);

        // 拉取通道的配置文件（以Orderer组织管理员的身份）
        CoreEnv ordererCoreEnv = fabricEnvService.buildCoreEnvForOrderer(selectedOrderer);
        File oldChannelConfig = ResourceUtils.createTempFile("json");
        ChannelUtils.fetchConfig(ordererCoreEnv, request.getChannelName(), oldChannelConfig);

        // 向通道配置文件中添加新Orderer的定义
        File tlsServerCrt = new File(selectedOrdererCertfileDir + "/tls/server.crt");
        ConfigtxOrderer configtxOrderer = new ConfigtxOrderer();
        configtxOrderer.setHost(request.getOrderer().getHost());
        configtxOrderer.setPort(request.getOrderer().getPort());
        configtxOrderer.setClientTlsCert(tlsServerCrt);
        configtxOrderer.setServerTlsCert(tlsServerCrt);
        File newChannelConfig = ResourceUtils.createTempFile("json");
        ChannelUtils.appendOrdererToChannelConfig(configtxOrderer, newChannelConfig);

        // 计算新旧JSON配置文件之间的差异得到Envelope，并对其进行签名
        File envelope = ResourceUtils.createTempFile("pb");
        ChannelUtils.generateEnvelope(request.getChannelName(), envelope, oldChannelConfig, newChannelConfig);
        ChannelUtils.signEnvelope(ordererCoreEnv.getMSPEnv(), envelope);

        // 将Envelope提交到现有的Orderer节点
        ChannelUtils.submitChannelUpdate(ordererCoreEnv.getMSPEnv(), ordererCoreEnv.getTLSEnv(), request.getChannelName(), envelope);

        // 将更新后的信息保存到数据库
        channel.getOrderers().add(newOrderer);
        channelRepo.save(channel);
    }

    public void submitInvitationCodes(ChannelSubmitInvitationCodesRequest request) throws Exception {
        String curOrgName = SecurityUtils.getUsername();
        ChannelEntity channel = getChannelOrThrowException(request.getChannelName());
        if (channel.getOrganizationNames().contains(curOrgName)) {
            throw new OrganizationException("组织已经是通道的合法成员");
        }

        // 对邀请码进行验证
        assertInvitationCodes(request.getInvitationCodes(), channel.getOrganizationNames(), curOrgName, channel.getName());

        // 从通道中随机选择一个Orderer
        Orderer orderer = RandomUtils.select(channel.getOrderers());

        // 拉取指定通道的配置
        CoreEnv ordererEnvVars = fabricEnvService.buildCoreEnvForOrderer(orderer);
        File oldChannelConfig = ResourceUtils.createTempFile("json");
        ChannelUtils.fetchConfig(ordererEnvVars, channel.getName(), oldChannelConfig);

        // 生成新组织的配置
        File newOrgConfigtxDir = ResourceUtils.createTempDir();
        File newOrgConfigtxYaml = new File(newOrgConfigtxDir + "configtx.yaml");
        File newOrgConfigtxJson = ResourceUtils.createTempFile("json");
        File newOrgCertfileDir = CertfileUtils.getCertfileDir(curOrgName, CertfileType.ADMIN);
        ConfigtxOrganization configtxOrganization = new ConfigtxOrganization();
        configtxOrganization.setId(curOrgName);
        configtxOrganization.setName(curOrgName);
        configtxOrganization.setMspDir(new File(newOrgCertfileDir + "/msp"));
        ConfigtxUtils.generateOrgConfigtx(newOrgConfigtxYaml, configtxOrganization);
        ConfigtxUtils.convertOrgConfigtxToJson(newOrgConfigtxJson, newOrgConfigtxDir, curOrgName);

        // 对通道配置文件进行更新并生成Envelope
        File newChannelConfig = ResourceUtils.createTempFile("json");
        FileUtils.copyFile(oldChannelConfig, newChannelConfig);
        ChannelUtils.appendOrganizationToAppChannelConfig(curOrgName, newOrgConfigtxJson, newChannelConfig);
        File envelope = ResourceUtils.createTempFile("pb");
        ChannelUtils.generateEnvelope(channel.getName(), envelope, oldChannelConfig, newChannelConfig);

        // 使用该通道中所有组织的身份对Envelope进行签名（包括未加入的组织）
        List<String> signerOrgNames = new ArrayList<>(channel.getOrganizationNames());
        signerOrgNames.add(curOrgName);
        signEnvelopeWithOrganizations(channel.getNetworkName(), signerOrgNames, envelope);

        // 将Envelope提交到Orderer
        MSPEnv organizationMspEnv = fabricEnvService.buildMSPEnvForOrg(channel.getNetworkName(), curOrgName);
        ChannelUtils.submitChannelUpdate(organizationMspEnv, ordererEnvVars.getTLSEnv(), channel.getName(), envelope);

        // 将组织保存至MongoDB
        channel.getOrganizationNames().add(curOrgName);
        channelRepo.save(channel);
    }

    public void joinChannel(ChannelJoinRequest request, MultipartFile peerCertZip) throws Exception {
        String curOrgName = SecurityUtils.getUsername();
        ChannelEntity channel = getChannelOrThrowException(request.getChannelName());

        // 对证书格式进行检查
        CertfileUtils.assertCertfileZip(peerCertZip);

        // 检查Peer是否在通道中且为该组织的节点
        Peer peer = (Peer) findNodeByAddress(request.getPeer(), channel.getPeers());
        if (peer != null) {
            throw new NodeException("相应的Peer节点已经加入了通道");
        }
        int newPeerNo = channel.getPeers().size();
        String newPeerId = IdentifierGenerator.ofPeer(channel.getName(), newPeerNo);
        peer = new Peer();
        peer.setName(newPeerId);
        peer.setHost(request.getPeer().getHost());
        peer.setPort(request.getPeer().getPort());
        peer.setOrganizationName(curOrgName);

        // 随机选择一个Orderer
        Orderer orderer = RandomUtils.select(channel.getOrderers());

        // 拉取通道的创世区块
        CoreEnv ordererEnvVars = fabricEnvService.buildCoreEnvForOrderer(orderer);
        File channelGenesisBlock = ResourceUtils.createTempFile("block");
        ChannelUtils.fetchGenesisBlock(ordererEnvVars, channel.getName(), channelGenesisBlock);

        // 解压Peer证书到临时目录
        File peerCertfileZip = ResourceUtils.createTempFile("zip");
        FileUtils.writeByteArrayToFile(peerCertfileZip, peerCertZip.getBytes());
        File peerCertTempCertfileDir = ResourceUtils.createTempDir();
        ZipUtils.unzip(peerCertfileZip, peerCertTempCertfileDir);

        // 尝试将Peer加入到通道中
        CoreEnv peerCoreEnv = fabricEnvService.buildCoreEnvForPeer(channel.getNetworkName(), curOrgName, peer);
        peerCoreEnv.setTlsRootCert(new File(peerCertTempCertfileDir + "/tls/ca.crt"));
        ChannelUtils.joinChannel(peerCoreEnv, channelGenesisBlock);

        // 将Peer证书保存到MinIO和证书目录
        minioService.putBytes(MinIOBucket.PEER_CERTFILE_BUCKET_NAME, peer.getName(), peerCertZip.getBytes());
        File peerCertfileDir = CertfileUtils.getCertfileDir(peer.getName(), CertfileType.PEER);
        ZipUtils.unzip(peerCertfileZip, peerCertfileDir);

        // 更新MongoDB中的信息
        channel.getPeers().add(peer);
        channelRepo.save(channel);
    }

    public InvitationCodeResult generateInvitationCode(ChannelGenerateInvitationCodeRequest request) throws Exception {
        // 检查组织是否是通道的合法成员
        String curOrgName = SecurityUtils.getUsername();
        ChannelEntity channel = getChannelOrThrowException(request.getChannelName());
        if (!channel.getOrganizationNames().contains(curOrgName)) {
            throw new OrganizationException("当前组织不存在于指定通道中");
        }

        // 检查被邀请的组织是否是合法的网络成员
        NetworkEntity network = networkService.getNetworkOrThrowException(channel.getNetworkName());
        if (!network.getOrganizationNames().contains(request.getOrganizationName())) {
            throw new OrganizationException("被邀请的组织不是合法的网络成员");
        }

        // 生成邀请码
        Invitation invitation = new Invitation();
        invitation.setInvitorOrgName(curOrgName);
        invitation.setInviteeOrgName(request.getOrganizationName());
        invitation.setChannelName(request.getChannelName());
        invitation.setTimestamp(System.currentTimeMillis());
        String invitationCode = InvitationUtils.getCode(invitation);
        return new InvitationCodeResult(invitationCode);
    }

    public void setAnchorPeer(ChannelSetAnchorPeerRequest request) throws Exception {
        String curOrgName = SecurityUtils.getUsername();
        ChannelEntity channel = getChannelOrThrowException(request.getChannelName());
        if (!channel.getOrganizationNames().contains(curOrgName)) {
            throw new OrganizationException("组织不是通道的合法成员");
        }

        // 检查Peer是否在通道中且为该组织的节点
        Peer peer = new Peer();
        peer.setHost(request.getPeer().getHost());
        peer.setPort(request.getPeer().getPort());
        peer.setOrganizationName(curOrgName);
        if (!channel.getPeers().contains(peer)) {
            throw new NodeNotFoundException("未找到相应的Peer节点（请先将其加入通道）");
        }

        // 从通道中随机选择一个Orderer
        Orderer orderer = RandomUtils.select(channel.getOrderers());
        // 拉取指定通道的配置
        CoreEnv ordererCoreEnv = fabricEnvService.buildCoreEnvForOrderer(orderer);
        File oldChannelConfig = ResourceUtils.createTempFile("json");
        ChannelUtils.fetchConfig(ordererCoreEnv, channel.getName(), oldChannelConfig);

        // 对通道配置文件进行更新并生成Envelope
        File newChannelConfig = ResourceUtils.createTempFile("json");
        FileUtils.copyFile(oldChannelConfig, newChannelConfig);
        ChannelUtils.appendAnchorPeerToChannelConfig(request.getPeer(), curOrgName, oldChannelConfig);
        File envelope = ResourceUtils.createTempFile("pb");
        ChannelUtils.generateEnvelope(channel.getName(), envelope, oldChannelConfig, newChannelConfig);

        // 使用该通道中所有组织的身份对Envelope进行签名
        signEnvelopeWithOrganizations(channel.getNetworkName(), channel.getOrganizationNames(), envelope);

        // 将Envelope提交到Orderer
        MSPEnv organizationMspEnv = fabricEnvService.buildMSPEnvForOrg(channel.getNetworkName(), curOrgName);
        ChannelUtils.submitChannelUpdate(organizationMspEnv, ordererCoreEnv.getTLSEnv(), channel.getName(), envelope);
    }

    public ResourceResult queryPeerTlsCert(ChannelQueryPeerTlsCertRequest request) throws Exception {
        // 检查网络是否存在
        Optional<ChannelEntity> channelOptional = channelRepo.findById(request.getChannelName());
        if (channelOptional.isEmpty()) {
            throw new ChannelException("不存在通道：" + request.getChannelName());
        }
        // 检查当前组织是否位于该网络中
        String curOrgName = SecurityUtils.getUsername();
        assertOrganizationInChannel(channelOptional.get(), curOrgName);

        String peerName = getPeerIdentifierOrThrowException(channelOptional.get(), request.getPeer());
        // 检查Orderer证书
        File ordererCertfileDir = CertfileUtils.getCertfileDir(peerName, CertfileType.PEER);
        CertfileUtils.assertCertfile(ordererCertfileDir);
        String downloadUrl = String.format("/download/cert/%s.crt", UUID.randomUUID());
        FileUtils.copyFile(new File(ordererCertfileDir + "/tls/ca.crt"), new File("static" + downloadUrl));

        ResourceResult result = new ResourceResult();
        result.setDownloadUrl(downloadUrl);
        return result;
    }

    public ChannelQueryPeerResult queryPeers(ChannelQueryPeerRequest request) throws Exception {
        Optional<ChannelEntity> channelOptional = channelRepo.findById(request.getChannelName());
        if (channelOptional.isPresent()) {
            ChannelQueryPeerResult result = new ChannelQueryPeerResult();
            List<Node> peers = new ArrayList<>(channelOptional.get().getPeers());
            result.setPeers(peers);
            return result;
        } else {
            throw new ChannelException("未找到通道：" + request.getChannelName());
        }
    }

    public ChannelQueryOrdererResult queryOrderers(ChannelQueryOrdererRequest request) throws Exception {
        Optional<ChannelEntity> channelOptional = channelRepo.findById(request.getChannelName());
        if (channelOptional.isPresent()) {
            ChannelQueryOrdererResult result = new ChannelQueryOrdererResult();
            List<Node> orderers = new ArrayList<>(channelOptional.get().getOrderers());
            result.setOrderers(orderers);
            return result;
        } else {
            throw new ChannelException("未找到通道：" + request.getChannelName());
        }
    }
}
