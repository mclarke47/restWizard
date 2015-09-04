package com.gmail.matthewclarke47.metadata;

import com.gmail.matthewclarke47.WebServiceAnnotations;
import com.gmail.matthewclarke47.parsing.ParametersParser;

import javax.ws.rs.Path;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class MethodMetaData {

    private String httpMethod;
    private String pathSuffix;
    private List<ParameterMetaData> parameterMetaData;

    public List<QueryParameterMetaData> getQueryParams() {
        return queryParams;
    }

    private List<QueryParameterMetaData> queryParams;

    public List<PathParameterMetaData> getPathParams() {
        return pathParams;
    }

    private List<PathParameterMetaData> pathParams;

    public String getResponseExample() {
        return responseExample;
    }

    private String responseExample;

    public MethodMetaData(String httpMethod, String pathSuffix, List<ParameterMetaData> parameterMetaData) {
        this.httpMethod = httpMethod;
        this.pathSuffix = pathSuffix;
        this.parameterMetaData = parameterMetaData;
        this.queryParams = parameterMetaData.stream()
                .filter(paramdata -> paramdata instanceof QueryParameterMetaData)
                .map(paramdata1 -> (QueryParameterMetaData) paramdata1)
                .collect(Collectors.toList());

        this.pathParams = parameterMetaData.stream()
                .filter(paramdata -> paramdata instanceof PathParameterMetaData)
                .map(paramdata1 -> (PathParameterMetaData) paramdata1)
                .collect(Collectors.toList());
        this.responseExample = createResponseExample(parameterMetaData);
    }

    private String createResponseExample(List<ParameterMetaData> parameterMetaDatas) {

            List<PropertyParameterMetaData> propertyParams = parameterMetaDatas.stream()
                    .filter(parameterMetaData -> parameterMetaData instanceof PropertyParameterMetaData)
                    .map(parameterMetaData -> (PropertyParameterMetaData) parameterMetaData)
                    .collect(Collectors.toList());

            if(propertyParams.isEmpty()){
                return "";
            }

            String str = "{\n\t";

            for (PropertyParameterMetaData property : propertyParams) {
                if (!str.equals("{\n\t")) {
                    str += ",\n\t";
                }
                str += "\"" + property.getKey() + "\": " + formatExample(property.getType());
            }
            str += "\n}";
            return str;

    }

    private String formatExample(Class<?> type) {

        if (type.equals(boolean.class)) {
            return "true";
        } else if (type.equals(int.class)) {
            return "12345";
        }
        return "\"ABC\"";
    }

    public static MethodMetaDataBuilder builder(Method method) {
        return new MethodMetaDataBuilder(method);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null || !(obj instanceof MethodMetaData))
            return false;

        MethodMetaData method = (MethodMetaData) obj;

        return this.pathSuffix.equals(method.getPathSuffix())
                && this.httpMethod.equals(method.getHttpMethod())
                && this.getParameterMetaData().size() == method.getParameterMetaData().size()
                && deepCheckParamsContains(method.getParameterMetaData());
    }

    private boolean deepCheckParamsContains(List<ParameterMetaData> parameterMetaDataList) {

        for (ParameterMetaData parameterMetaData : this.getParameterMetaData()) {
            if (!parameterMetaDataList.contains(parameterMetaData)) {
                return false;
            }
        }
        return true;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPathSuffix() {
        return pathSuffix;
    }

    public List<ParameterMetaData> getParameterMetaData() {
        return parameterMetaData;
    }

    public static class MethodMetaDataBuilder {

        private String httpMethod;
        private String pathSuffix;
        private List<ParameterMetaData> parameterMetaData;

        private MethodMetaDataBuilder(Method method) {
            this.pathSuffix = method.getAnnotation(Path.class).value();
            this.httpMethod = getHttpMethod(method);
            this.parameterMetaData = parseParameterMetaData(method);
        }

        private String getHttpMethod(Method method) {

            return WebServiceAnnotations.HTTP_METHOD_TO_TEXT
                    .getOrDefault(WebServiceAnnotations.HTTP_METHOD_TO_TEXT
                            .keySet()
                            .stream()
                            .filter(method::isAnnotationPresent)
                            .findFirst().get(), "");
        }

        private List<ParameterMetaData> parseParameterMetaData(Method method) {

            return new ParametersParser(method).getMetaData();
        }

        public MethodMetaData build() {
            return new MethodMetaData(this.httpMethod, this.pathSuffix, this.parameterMetaData);
        }
    }
}
