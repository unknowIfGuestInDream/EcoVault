#!/usr/bin/env groovy

// EcoVault Jenkins 持续集成流水线
//
// 本流水线执行以下步骤：
// 1. 检出代码
// 2. 编译与测试
// 3. 代码格式检查
// 4. 生成 JaCoCo 覆盖率报告
// 5. 生成 Doxygen 文档
// 6. 归档构建产物
// 7. 部署到生产环境（仅 master 分支）

pipeline {
    agent any

    environment {
        // Java 环境配置
        JAVA_HOME = '/usr/lib/jvm/temurin-25-jdk-amd64'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
        
        // Maven 配置
        MAVEN_OPTS = '-Xmx1024m -XX:MaxPermSize=256m'
        
        // PlantUML JAR 路径（用于 Doxygen 生成 UML 图）
        PLANTUML_JAR_PATH = '/opt/plantuml/plantuml.jar'
        
        // 项目信息
        PROJECT_NAME = 'EcoVault'
        ARTIFACT_NAME = 'ecovault.jar'
        
        // 部署配置
        DEPLOY_HOST = 'localhost'
        DEPLOY_PORT = '8080'
        DEPLOY_DIR = '/opt/ecovault'
    }

    options {
        // 保留最近 10 次构建
        buildDiscarder(logRotator(numToKeepStr: '10'))
        
        // 构建超时 30 分钟
        timeout(time: 30, unit: 'MINUTES')
        
        // 禁用并发构建
        disableConcurrentBuilds()
        
        // 添加构建时间戳
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "========== 检出代码 =========="
                    checkout scm
                    
                    // 显示 Git 信息
                    sh '''
                        echo "Git Branch: $(git rev-parse --abbrev-ref HEAD)"
                        echo "Git Commit: $(git rev-parse HEAD)"
                        echo "Git Author: $(git log -1 --pretty=format:'%an <%ae>')"
                        echo "Git Message: $(git log -1 --pretty=format:'%s')"
                    '''
                }
            }
        }

        stage('Build and Test') {
            steps {
                script {
                    echo "========== 编译与测试 =========="
                    sh '''
                        mvn -B --no-transfer-progress clean verify
                    '''
                }
            }
            post {
                always {
                    // 发布 JUnit 测试报告
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Code Format Check') {
            steps {
                script {
                    echo "========== 代码格式检查 =========="
                    sh '''
                        mvn spring-javaformat:validate
                    '''
                }
            }
        }

        stage('JaCoCo Coverage Report') {
            steps {
                script {
                    echo "========== 生成覆盖率报告 =========="
                    // JaCoCo 报告已在 verify 阶段生成
                    sh '''
                        if [ -f target/site/jacoco/jacoco.csv ]; then
                            echo "覆盖率报告已生成"
                            cat target/site/jacoco/jacoco.csv | head -n 5
                        else
                            echo "警告: 覆盖率报告未生成"
                        fi
                    '''
                }
            }
            post {
                always {
                    // 发布 JaCoCo 覆盖率报告
                    jacoco execPattern: 'target/jacoco.exec', classPattern: 'target/classes', sourcePattern: 'src/main/java'
                }
            }
        }

        stage('Generate Doxygen Docs') {
            steps {
                script {
                    echo "========== 生成 Doxygen 文档 =========="
                    def hasDoxygen = sh(script: 'command -v doxygen >/dev/null 2>&1', returnStatus: true) == 0
                    if (hasDoxygen) {
                        sh 'rm -rf docs-gen doxygen-docs.zip'
                        sh '''
                            if [ ! -f "$PLANTUML_JAR_PATH" ]; then
                                echo "PlantUML jar not found at $PLANTUML_JAR_PATH; running Doxygen without PlantUML diagrams."
                                unset PLANTUML_JAR_PATH
                            fi
                            doxygen doxygen/Doxyfile
                        '''
                        sh 'cd docs-gen && zip -qr ../doxygen-docs.zip html'
                        archiveArtifacts artifacts: 'doxygen-docs.zip', allowEmptyArchive: false
                        echo "Doxygen 文档已生成并归档"
                    } else {
                        echo 'doxygen not found on this agent; skip Doxygen documentation generation'
                    }
                }
            }
            post {
                cleanup {
                    sh 'rm -rf docs-gen doxygen-docs.zip || true'
                }
            }
        }

        stage('Package') {
            steps {
                script {
                    echo "========== 打包应用 =========="
                    sh '''
                        mvn -B --no-transfer-progress package -DskipTests
                    '''
                }
            }
            post {
                always {
                    // 归档构建产物
                    archiveArtifacts artifacts: "target/${ARTIFACT_NAME}", allowEmptyArchive: false
                }
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'master'
            }
            steps {
                script {
                    echo "========== 部署到生产环境 =========="
                    sh '''
                        # 调用部署脚本
                        if [ -f deploy/deploy.sh ]; then
                            bash deploy/deploy.sh
                        else
                            echo "警告: deploy/deploy.sh 不存在，跳过部署"
                        fi
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "========== 构建成功 =========="
            // 可以在这里添加通知，例如发送邮件或消息
        }
        failure {
            echo "========== 构建失败 =========="
            // 可以在这里添加失败通知
        }
        always {
            echo "========== 清理工作空间 =========="
            // 清理临时文件
            cleanWs(
                deleteDirs: true,
                patterns: [
                    [pattern: 'target', type: 'INCLUDE'],
                    [pattern: 'docs-gen', type: 'INCLUDE']
                ]
            )
        }
    }
}
