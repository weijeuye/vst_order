
/*
 * 
 */

package com.lvmama.vst.order.webservice.contract;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lvmama.vst.comm.utils.ExceptionFormatUtil;
import com.lvmama.vst.comm.vo.Constant;
import com.lvmama.vst.order.service.book.OrderBookServiceImpl;

/**
 * This class was generated by Apache CXF 2.1.4
 * Tue Dec 02 16:01:22 CST 2014
 * Generated source version: 2.1.4
 * 
 */


@WebServiceClient(name = "ContractWebService", 
                  wsdlLocation = "http://test.contract-api.palmyou.com/ntsms-contract-api/service/ContractWebService?wsdl",
                  targetNamespace = "http://www.springframework.org/schema/beans") 
public class ContractWSService extends Service {
	private static final Logger logger = LoggerFactory.getLogger(ContractWSService.class);
    public final static URL WSDL_LOCATION;
    public final static QName SERVICE = new QName("http://www.springframework.org/schema/beans", "ContractWebService");
    public final static QName ContractWebServicePort = new QName("http://www.springframework.org/schema/beans", "ContractWebServicePort");
    static {
    	String urlStr = Constant.getInstance().getContractSyncUrl();
        URL url = null;
        try {
            //url = new URL("http://test.contract-api.palmyou.com/ntsms-contract-api/service/ContractWebService?wsdl");
        	url = new URL(urlStr);
        } catch (MalformedURLException e) {
            //System.err.println("Can not initialize the default wsdl from http://test.contract-api.palmyou.com/ntsms-contract-api/service/ContractWebService?wsdl");
        	logger.error(ExceptionFormatUtil.getTrace(e));
        	logger.error("Can not initialize the wsdl from " + urlStr);
        }
        WSDL_LOCATION = url;
    }

    public ContractWSService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public ContractWSService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public ContractWSService() {
        super(WSDL_LOCATION, SERVICE);
    }

    /**
     * 
     * @return
     *     returns ContractWebService
     */
    @WebEndpoint(name = "ContractWebServicePort")
    public ContractWebService getContractWebServicePort() {
        return super.getPort(ContractWebServicePort, ContractWebService.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ContractWebService
     */
    @WebEndpoint(name = "ContractWebServicePort")
    public ContractWebService getContractWebServicePort(WebServiceFeature... features) {
        return super.getPort(ContractWebServicePort, ContractWebService.class, features);
    }

}
