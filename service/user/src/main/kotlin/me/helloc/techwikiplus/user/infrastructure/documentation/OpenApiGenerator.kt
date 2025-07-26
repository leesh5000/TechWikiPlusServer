package me.helloc.techwikiplus.user.infrastructure.documentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File

/**
 * restdocs-api-spec의 resource.json 파일들을 읽어서 OpenAPI 3.0 스펙으로 변환
 */
object OpenApiGenerator {
    private val objectMapper = ObjectMapper()

    fun generateOpenApiSpec(snippetsDir: File, title: String, description: String, version: String, serverUrl: String): String {
        val openApiSpec = objectMapper.createObjectNode()

        // OpenAPI 기본 정보
        openApiSpec.put("openapi", "3.0.1")

        val infoNode = objectMapper.createObjectNode()
        infoNode.put("title", title)
        infoNode.put("description", description)
        infoNode.put("version", version)
        openApiSpec.set<ObjectNode>("info", infoNode)

        // 서버 정보
        val serversArray = objectMapper.createArrayNode()
        val serverNode = objectMapper.createObjectNode()
        serverNode.put("url", serverUrl)
        serverNode.put("description", "Local development server")
        serversArray.add(serverNode)
        openApiSpec.set<ArrayNode>("servers", serversArray)

        // paths 노드
        val pathsNode = objectMapper.createObjectNode()

        // components 노드
        val componentsNode = objectMapper.createObjectNode()
        val schemasNode = objectMapper.createObjectNode()

        // 에러 응답 스키마 추가
        val errorResponseSchema = objectMapper.createObjectNode()
        errorResponseSchema.put("type", "object")
        val errorPropertiesNode = objectMapper.createObjectNode()
        errorPropertiesNode.set<ObjectNode>("errorCode", objectMapper.createObjectNode().apply {
            put("type", "string")
            put("description", "에러 코드")
        })
        errorPropertiesNode.set<ObjectNode>("message", objectMapper.createObjectNode().apply {
            put("type", "string")
            put("description", "에러 메시지")
        })
        errorPropertiesNode.set<ObjectNode>("timestamp", objectMapper.createObjectNode().apply {
            put("type", "string")
            put("description", "에러 발생 시간")
        })
        errorPropertiesNode.set<ObjectNode>("path", objectMapper.createObjectNode().apply {
            put("type", "string")
            put("description", "요청 경로")
        })
        errorPropertiesNode.set<ObjectNode>("localizedMessage", objectMapper.createObjectNode().apply {
            put("type", "string")
            put("description", "현지화된 메시지")
        })
        errorPropertiesNode.set<ObjectNode>("details", objectMapper.createObjectNode().apply {
            put("type", "object")
            put("description", "추가 에러 정보")
        })
        errorResponseSchema.set<ObjectNode>("properties", errorPropertiesNode)
        schemasNode.set<ObjectNode>("ErrorResponse", errorResponseSchema)

        // snippets 디렉토리에서 모든 resource.json 파일 찾기
        snippetsDir.walk()
            .filter { it.name == "resource.json" }
            .forEach { resourceFile ->
                try {
                    val resource = objectMapper.readTree(resourceFile)
                    val path = resource["request"]["path"].asText()
                    val method = resource["request"]["method"].asText().lowercase()

                    // path가 없으면 생성
                    val pathNode = pathsNode.get(path) as? ObjectNode ?: objectMapper.createObjectNode()

                    // method 노드 생성
                    val methodNode = objectMapper.createObjectNode()
                    methodNode.put("summary", resource["summary"].asText())
                    methodNode.put("description", resource["description"].asText())
                    methodNode.put("operationId", resource["operationId"].asText())

                    // tags
                    val tagsArray = objectMapper.createArrayNode()
                    tagsArray.add("User")
                    methodNode.set<ArrayNode>("tags", tagsArray)

                    // request body
                    if (resource["request"]["requestFields"].size() > 0) {
                        val requestBodyNode = objectMapper.createObjectNode()
                        requestBodyNode.put("required", true)
                        val contentNode = objectMapper.createObjectNode()
                        val jsonContentNode = objectMapper.createObjectNode()
                        val schemaNode = objectMapper.createObjectNode()
                        schemaNode.put("type", "object")

                        val propertiesNode = objectMapper.createObjectNode()
                        val requiredArray = objectMapper.createArrayNode()

                        resource["request"]["requestFields"].forEach { field ->
                            val fieldName = field["path"].asText()
                            val fieldNode = objectMapper.createObjectNode()
                            fieldNode.put("type", field["type"].asText().lowercase())
                            fieldNode.put("description", field["description"].asText())
                            propertiesNode.set<ObjectNode>(fieldName, fieldNode)
                            if (!field["optional"].asBoolean()) {
                                requiredArray.add(fieldName)
                            }
                        }

                        schemaNode.set<ObjectNode>("properties", propertiesNode)
                        schemaNode.set<ArrayNode>("required", requiredArray)
                        jsonContentNode.set<ObjectNode>("schema", schemaNode)
                        contentNode.set<ObjectNode>("application/json", jsonContentNode)
                        requestBodyNode.set<ObjectNode>("content", contentNode)
                        methodNode.set<ObjectNode>("requestBody", requestBodyNode)
                    }

                    // responses
                    val responsesNode = objectMapper.createObjectNode()
                    val statusCode = resource["response"]["status"].asText()
                    val responseNode = objectMapper.createObjectNode()
                    responseNode.put("description", if (statusCode == "202") "요청 성공" else "요청 실패")

                    // response headers
                    if (resource["response"]["headers"].size() > 0) {
                        val headersNode = objectMapper.createObjectNode()
                        resource["response"]["headers"].forEach { header ->
                            val headerNode = objectMapper.createObjectNode()
                            headerNode.put("description", header["description"].asText())
                            val schemaNode = objectMapper.createObjectNode()
                            schemaNode.put("type", header["type"].asText().lowercase())
                            headerNode.set<ObjectNode>("schema", schemaNode)
                            headersNode.set<ObjectNode>(header["name"].asText(), headerNode)
                        }
                        responseNode.set<ObjectNode>("headers", headersNode)
                    }

                    // response body
                    if (resource["response"]["responseFields"].size() > 0) {
                        val contentNode = objectMapper.createObjectNode()
                        val jsonContentNode = objectMapper.createObjectNode()
                        val schemaNode = objectMapper.createObjectNode()
                        schemaNode.put("\$ref", "#/components/schemas/ErrorResponse")
                        jsonContentNode.set<ObjectNode>("schema", schemaNode)
                        contentNode.set<ObjectNode>("application/json", jsonContentNode)
                        responseNode.set<ObjectNode>("content", contentNode)
                    }

                    responsesNode.set<ObjectNode>(statusCode, responseNode)
                    methodNode.set<ObjectNode>("responses", responsesNode)

                    pathNode.set<ObjectNode>(method, methodNode)
                    pathsNode.set<ObjectNode>(path, pathNode)
                } catch (e: Exception) {
                    println("Error processing resource file ${resourceFile.path}: ${e.message}")
                }
            }

        openApiSpec.set<ObjectNode>("paths", pathsNode)
        componentsNode.set<ObjectNode>("schemas", schemasNode)
        openApiSpec.set<ObjectNode>("components", componentsNode)

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(openApiSpec)
    }
}