workspace "edda21" "Educational platform microservices system focused on asynchronous question generation" {

    !identifiers hierarchical

    model {
        instructor = person "Instructor" "Requests question generation and reads assignment content"
        student = person "Student" "Authenticates and will work on assignments in the target product"

        edda21 = softwareSystem "edda21" "Java and Spring Boot microservices platform for EdTech question generation" {
            apiGateway = container "API Gateway" "Public HTTP entry point" "Spring Cloud Gateway"
            backend = container "Backend" "Authentication and generation session orchestration" "Spring Boot"
            questionProvider = container "Question Provider" "Worker logic, question persistence, and assignment query" "Spring Boot"
            llmBridge = container "LLM Bridge" "Internal question generation service" "Spring Boot + Spring AI"
            postgres = container "PostgreSQL" "Relational storage" "PostgreSQL"
            redpanda = container "Redpanda" "Asynchronous messaging" "Kafka-compatible broker"
            serviceRegistry = container "Service Registry" "Service discovery" "Eureka"
            configServer = container "Config Server" "Central configuration" "Spring Cloud Config"
        }

        instructor -> edda21.apiGateway "Uses over HTTP"
        student -> edda21.apiGateway "Uses over HTTP"

        edda21.apiGateway -> edda21.backend "Routes requests to"
        edda21.backend -> edda21.postgres "Reads and writes relational data"
        edda21.backend -> edda21.redpanda "Publishes generation requests"
        edda21.questionProvider -> edda21.redpanda "Consumes generation requests and response payloads"
        edda21.questionProvider -> edda21.llmBridge "Calls internal question generation API"
        edda21.questionProvider -> edda21.postgres "Reads and writes questions and assignment links"

        edda21.apiGateway -> edda21.serviceRegistry "Discovers services through"
        edda21.backend -> edda21.serviceRegistry "Registers with"
        edda21.questionProvider -> edda21.serviceRegistry "Registers with"
        edda21.llmBridge -> edda21.serviceRegistry "Registers with"

        edda21.apiGateway -> edda21.configServer "Loads routes from"
        edda21.backend -> edda21.configServer "Loads configuration from"
        edda21.questionProvider -> edda21.configServer "Loads configuration from"
        edda21.llmBridge -> edda21.configServer "Loads configuration from"

        instructor -> edda21 "Uses to manage question generation workflows"
        student -> edda21 "Uses to access generated learning content in the target product"
    }

    views {
        systemContext edda21 "SystemContext" "External actors and edda21 system boundary" {
            include instructor
            include student
            include edda21
            autoLayout lr
            title "edda21 - System Context"
        }

        container edda21 "Containers" "Main runtime containers for edda21" {
            include *
            include instructor
            include student
            autoLayout lr
            title "edda21 - Containers"
        }

        styles {
            element "Person" {
                shape person
                background #08427b
                color #ffffff
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Database" {
                shape cylinder
            }
            element "Message Bus" {
                shape pipe
            }
        }

        themes default
    }
}
