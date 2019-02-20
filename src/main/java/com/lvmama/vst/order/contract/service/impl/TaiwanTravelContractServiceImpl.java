package com.lvmama.vst.order.contract.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lvmama.vst.back.client.prod.service.ProdProductClientService;
import com.lvmama.vst.back.goods.vo.ProdProductParam;
import com.lvmama.vst.back.order.po.OrdOrder;
import com.lvmama.vst.back.order.po.OrdOrderItem;
import com.lvmama.vst.back.order.po.OrdPerson;
import com.lvmama.vst.back.order.po.OrdTravelContract;
import com.lvmama.vst.back.prod.po.ProdProduct;
import com.lvmama.vst.comm.utils.DateUtil;
import com.lvmama.vst.comm.utils.ExceptionFormatUtil;
import com.lvmama.vst.comm.utils.pdf.PdfUtil;
import com.lvmama.vst.comm.vo.ResultHandle;
import com.lvmama.vst.comm.vo.ResultHandleT;
import com.lvmama.vst.comm.vo.econtract.TravelContractVO;
import com.lvmama.vst.comm.vo.order.OrderMonitorRst;
import com.lvmama.vst.order.contract.service.IOrderElectricContactService;
import com.lvmama.vst.order.contract.vo.OutboundTourContractVO;
import com.lvmama.vst.order.service.IComplexQueryService;
import com.lvmama.vst.order.service.IOrdTravelContractService;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * 赴台旅游服务
 * @author chenpingfan
 *
 */
@Service("taiwanTravelContractService")
public class TaiwanTravelContractServiceImpl extends AbstractOrderTravelElectricContactService implements IOrderElectricContactService{

	private static final Log LOG = LogFactory.getLog(TaiwanTravelContractServiceImpl.class);
	@Autowired
	private IComplexQueryService complexQueryService;
	
	@Autowired
	private IOrdTravelContractService ordTravelContractService;
	
	@Autowired
	private ProdProductClientService prodProductClientService;
	
	private static final String contractName = "赴台旅游预订须知";
	
	private static final String SERVER_TYPE = "COM_AFFIX";
	/*赴台旅游合同模板**/
	private static final String templateName = "taiwanlvyouhetong.ftl";
	
	@Override
	public ResultHandle saveTravelContact(OrdTravelContract ordTravelContract,
			String operatorName) {
		ResultHandle resultHandle = new ResultHandle();
		if(null != ordTravelContract){
			OrdOrder order = complexQueryService.queryOrderByOrderId(ordTravelContract.getOrderId());
			if (order == null) {
				resultHandle.setMsg("订单ID=" + ordTravelContract.getOrderId() + "不存在。");
				return resultHandle;
			}
			List<OrdOrderItem> ordOrderItemList =order.getOrderItemList();
			if (ordOrderItemList == null || ordOrderItemList.isEmpty()) {
				resultHandle.setMsg("订单ID=" + order.getOrderId() + "不存在子订单。");
				return resultHandle;
			}
			
			try 
			{
				List<OrdTravelContract> list = new ArrayList<OrdTravelContract>();
				list.add(ordTravelContract);
				order.setOrdTravelContractList(list);
				
				File directioryFile = initDirectory();
				if (directioryFile == null || !directioryFile.exists()) {
					resultHandle.setMsg("合同模板目录不存在。");
					return resultHandle;
				}
				Map<String,Object> rootMap=this.captureContract(ordTravelContract,order,directioryFile);
				TravelContractVO travelContractVO =(TravelContractVO)rootMap.get("travelContractVO");
				Configuration configuration = initConfiguration(directioryFile);
				if (configuration == null) {
					resultHandle.setMsg("初始化freemarker失败。");
					return resultHandle;
				}
				
				Template template = configuration.getTemplate(templateName.toString());
				if (template == null) {
					resultHandle.setMsg("初始化ftl模板失败。");
					return resultHandle;
				}
				
				StringWriter sw = new StringWriter();
				template.process(rootMap, sw);
				String htmlString = sw.toString();
				if (htmlString == null) {
					resultHandle.setMsg("合同HTML生成失败。");
					return resultHandle;
				}
				
				ByteArrayOutputStream bao = PdfUtil.createPdfFile(htmlString);
				if (bao == null) {
					resultHandle.setMsg("合同PDF生成失败。");
					return resultHandle;
				}
				
				byte[] fileBytes = bao.toByteArray();
				bao.close();
				
				String fileName = null;
				
				if(StringUtils.isNotEmpty(travelContractVO.getContractVersion())){
					fileName = "taiwanTravelContract_" + travelContractVO.getContractVersion() + ".pdf";
				}else{
					fileName = "taiwanTravelContract_emptyTemplate.pdf";
				}
				
				
				//调试时打开
				this.newContractDebug(fileBytes, fileName);
				
				boolean isCreateOrder=false;
				if (ordTravelContract.getFileId()==null) {
					isCreateOrder=true;
				}
				
				ByteArrayInputStream bai = new ByteArrayInputStream(fileBytes);
				Long fileId = fsClient.uploadFile(fileName, bai, SERVER_TYPE);
				bai.close();
				
				if (fileId != null && fileId != 0) {
					ordTravelContract.setVersion(travelContractVO.getContractVersion());
					ordTravelContract.setFileId(fileId);					
					//合同签约状态逻辑
					setOrdContractStatus(ordTravelContract, order,
							isCreateOrder);
					ordTravelContract.setContractName(contractName.toString());
					ordTravelContract.setCreateTime(new Date());
					if (ordTravelContractService.updateByPrimaryKeySelective(ordTravelContract, operatorName) <= 0) {
						ordTravelContractService.saveOrdTravelContract(ordTravelContract, operatorName);
					}
					
					String content=contractName+"更新成功";
					if (isCreateOrder) {
						content=contractName+"生成成功";
					}
					this.insertOrderLog(ordTravelContract.getOrderId(), ordTravelContract.getOrdContractId(), operatorName, content,  null);
				}else{
					resultHandle.setMsg("合同上传失败。");
				}
				
			} catch (Exception e) 
			{
				LOG.error(ExceptionFormatUtil.getTrace(e));
				resultHandle.setMsg(e);
			}
		}
		return resultHandle;
	}

	@Override
	public ResultHandle updateTravelContact(OutboundTourContractVO contractVO,
			String operatorName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultHandle sendEcontractWithEmail(
			OrdTravelContract ordTravelContract) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> captureContract(OrdTravelContract ordTravelContract, OrdOrder order,File directioryFile) {
	Map<String,Object> rootMap = new HashMap<String, Object>();	
	TravelContractVO travelContractVO = null;
	Map<String,List<OrderMonitorRst>>  chidOrderMap = null;
	
		if (order != null) {		
			
			//针对下单后置的订单
			//条件 1.订单后置 2.游玩人未锁定  ，下载合同是空模板，反之则显示有值的正常模板
			if(("Y").equals(order.getTravellerDelayFlag()) && ("N").equals(order.getTravellerLockFlag())){
				travelContractVO = new TravelContractVO();
				chidOrderMap = new HashMap<String, List<OrderMonitorRst>>();
			}else{
				travelContractVO = buildTravelContractVOData(ordTravelContract, order);
				chidOrderMap=findChildOrderList(ordTravelContract,order,false);
			}
			
			rootMap.put("order", order);
			LOG.info("TaiwanTravelContractServiceImpl.saveTravelContact,fileDir=" + travelContractVO.getTemplateDirectory());
			rootMap.put("travelContractVO", travelContractVO);
			rootMap.put("chidOrderMap", chidOrderMap);			
		}	
		return rootMap;
	}
	
	/**
	 * 转换合同模板查看
	 */
	@Override
	public ResultHandleT<String> getContractTemplateHtml() {
		return contractTemplateHtml(templateName.toString());
	}

	@Override
	public ResultHandle updateTravelContact(TravelContractVO travelContractVO,
			OrdOrder order, OrdTravelContract ordTravelContract,
			String operatorName) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 组装合同数据
	 * @param ordTravelContract
	 * @param order
	 * @return
	 */
	private TravelContractVO buildTravelContractVOData(OrdTravelContract ordTravelContract,OrdOrder order){
		Long productId=order.getProductId();
		String productName=order.getOrderProductName();		
		TravelContractVO travelContractVO = null;
		
		ProdProductParam param = new ProdProductParam();
		param.setProductProp(true);
		param.setProductBranchValue(true);
		param.setProdEcontract(true);
		
	   ResultHandleT<ProdProduct> resultHandle=this.prodProductClientService.findLineProductByProductId(productId, param);
		
		ProdProduct prodProduct=resultHandle.getReturnContent();
		if (order != null && prodProduct != null){
			travelContractVO = new TravelContractVO();
			travelContractVO.setProdProduct(prodProduct);
			travelContractVO.setProductId(productId);
			String appendVersion = getAppendVersion(ordTravelContract);
			
			//合同编号
			String version = DateUtil.formatDate(order.getVisitTime(), "yyyyMMdd") + "-" + order.getOrderId() + "-" + appendVersion;
			travelContractVO.setContractVersion(version);
			
			//订单编号
			travelContractVO.setOrderId(order.getOrderId().toString());
			travelContractVO.setProductName(productName);
			
			OrdPerson contactPerson=order.getContactPerson();
			if (contactPerson!=null) {
			
				//甲方
				travelContractVO.setTravellers(contactPerson.getFullName());
				
				//联系电话
				travelContractVO.setContactTelePhoneNo(order.getContactPerson().getMobile());
				
			}	
			//乙方 出境社
			travelContractVO.setFilialeName(this.filialeNameMap.get(order.getFilialeName()));
			//许可证编号
			travelContractVO.setPermit(this.permitlMap.get(order.getFilialeName()));		
			//产品名称
			travelContractVO.setProductName(productName);
			//监督电话
			travelContractVO.setJianduTel(this.jianduTelMap.get(order.getFilialeName()));			
			//旅行社盖章
			travelContractVO.setStampImage(getStampImageNameByFilialeName(prodProduct.getFiliale()));		
		}
		return travelContractVO;	
	}
	
}
