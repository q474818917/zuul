package filters.pre

import com.alibaba.dubbo.config.ReferenceConfig
import com.alibaba.dubbo.config.utils.ReferenceConfigCache
import com.alibaba.dubbo.rpc.service.GenericService
import com.google.common.collect.Lists
import com.netflix.zuul.ZuulFilter
import com.netflix.zuul.context.RequestContext
import com.netflix.zuul.exception.ZuulException

import javax.servlet.http.HttpServletRequest

class DubboFilter extends ZuulFilter {

    @Override
    String filterType() {
        return 'pre'
    }

    @Override
    int filterOrder() {
        return 2
    }

    @Override
    boolean shouldFilter() {
        HttpServletRequest req = RequestContext.currentContext.request as HttpServletRequest
        if(req.getRequestURI().contains("dubbo"))
            return true
        return false
    }

    @Override
    Object run() throws ZuulException {
        final String serviceName = "io.github.tesla.dubbo.user.UserService";
        final String methodName = "sayHello";
        final String group = "tesla";
        final String version = "1.0.0";
        ReferenceConfig<GenericService> reference = new ReferenceConfig<GenericService>();
        reference.setApplication(applicationConfig);
        reference.setRegistry(registryConfig);
        reference.setInterface(serviceName);
        reference.setGroup(group);
        reference.setGeneric(true);
        reference.setCheck(false);
        reference.setVersion(version);
        ReferenceConfigCache cache = ReferenceConfigCache.getCache();
        GenericService genericService = cache.get(reference);
        String templateKey = this.cacheTemplate(rpcDo);
        Pair<String[], Object[]> typeAndValue = this.transformerData(templateKey, servletRequest);
        Object response =
                genericService.$invoke(methodName, typeAndValue.getLeft(), typeAndValue.getRight());
        return null
    }

    private Pair<String[], Object[]> transformerData(String templateKey,
                                                     final NettyHttpServletRequest servletRequest) throws TemplateNotFoundException,
            MalformedTemplateNameException, ParseException, IOException, TemplateException {
        String outPutJson = this.doDataMapping(templateKey, servletRequest);
        Map<String, String> dubboParamters =
                JSON.parseObject(outPutJson, new TypeReference<HashMap<String, String>>() {});
        List<String> type = Lists.newArrayList();
        List<Object> value = Lists.newArrayList();
        for (Map.Entry<String, String> entry : dubboParamters.entrySet()) {
            String type_ = entry.getKey();
            String value_ = entry.getValue();
            type.add(type_);
            if (type_.startsWith("java")) {
                value.add(value_);
            } else {
                Map<String, String> value_map =
                        JSON.parseObject(value_, new TypeReference<HashMap<String, String>>() {});
                value_map.put("class", type_);
                value.add(value_map);
            }
        }
        String[] typeArray = new String[type.size()];
        type.toArray(typeArray);
        return new ImmutablePair<String[], Object[]>(typeArray, value.toArray());
    }
}
