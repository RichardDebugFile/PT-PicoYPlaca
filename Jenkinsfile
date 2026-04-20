// ============================================================================
// PT-PicoYPlaca - Jenkinsfile
// Pipeline CI/CD con las 4 estrategias de optimización
//
// ESTRATEGIA 1: Pipeline dividido en etapas (stages)
// ESTRATEGIA 2: Pipeline modular y reutilizable (funciones compartidas)
// ESTRATEGIA 3: CI por rama (Multibranch Pipeline - detecta todas las ramas)
// ESTRATEGIA 4: Paralelismo (etapas ejecutadas en paralelo)
// ============================================================================

// -------------------------------------------------
// ESTRATEGIA 2: Funciones reutilizables
// Estas funciones se pueden invocar desde cualquier
// stage, evitando duplicación de código.
// -------------------------------------------------
def runMaven(String goals) {
    dir('backend') {
        sh "mvn ${goals} --no-transfer-progress"
    }
}

def runNpm(String command) {
    dir('frontend') {
        sh "npm ${command}"
    }
}

pipeline {
    agent any

    // Variables de entorno globales
    environment {
        JAVA_HOME_TOOL   = 'JDK17'       // Nombre configurado en Jenkins Global Tool Configuration
        MAVEN_HOME_TOOL  = 'Maven3'       // Nombre configurado en Jenkins Global Tool Configuration
        NODEJS_HOME_TOOL = 'NodeJS18'     // Nombre configurado en Jenkins Global Tool Configuration
    }

    // Opciones del pipeline
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))   // Mantener solo 5 builds
        timestamps()                                     // Mostrar timestamps en los logs
        timeout(time: 30, unit: 'MINUTES')               // Timeout global de 30 minutos
    }

    // =========================================================================
    // ESTRATEGIA 3: CI por rama
    // Este Jenkinsfile se ejecuta automáticamente en CUALQUIER rama
    // (main, develop, feature/*, bugfix/*, etc.) gracias al Multibranch Pipeline.
    // La variable env.BRANCH_NAME contiene el nombre de la rama actual.
    // =========================================================================

    // =========================================================================
    // ESTRATEGIA 1: Pipeline dividido en etapas
    // Cada etapa tiene una responsabilidad clara y separada.
    // =========================================================================
    stages {

        // --- ETAPA 1: Checkout del código fuente ---
        stage('Checkout') {
            steps {
                echo "=== Rama actual: ${env.BRANCH_NAME} ==="
                checkout scm
            }
        }

        // =====================================================================
        // ESTRATEGIA 4: Paralelismo
        // Las pruebas y análisis de Backend y Frontend se ejecutan EN PARALELO
        // para reducir el tiempo total del pipeline.
        // =====================================================================
        stage('Tests y Análisis - Paralelo') {
            parallel {

                // --- BACKEND: Tests automáticos ---
                stage('Backend - Tests') {
                    steps {
                        echo '>>> Ejecutando tests unitarios e integración del Backend...'
                        // ESTRATEGIA 2: Uso de función reutilizable
                        script {
                            runMaven('test')
                        }
                    }
                    post {
                        always {
                            // Publicar resultados de tests JUnit
                            junit allowEmptyResults: true,
                                 testResults: 'backend/target/surefire-reports/*.xml'
                        }
                    }
                }

                // --- FRONTEND: Instalación y Lint ---
                stage('Frontend - Lint') {
                    steps {
                        echo '>>> Instalando dependencias y ejecutando linter del Frontend...'
                        script {
                            // ESTRATEGIA 2: Uso de función reutilizable
                            runNpm('ci')
                            // ESLint para verificar estilo de código
                            // (requiere @angular-eslint configurado en el proyecto)
                            runNpm('run lint --if-present || true')
                        }
                    }
                }
            }
        }

        // --- ETAPA 3: Análisis de calidad de código del Backend ---
        stage('Backend - Checkstyle') {
            steps {
                echo '>>> Ejecutando análisis de calidad de código (Checkstyle)...'
                script {
                    // Checkstyle para verificar estilo de código Java
                    runMaven('checkstyle:checkstyle -Dcheckstyle.failOnViolation=false')
                }
            }
            post {
                always {
                    // Archivar el reporte de Checkstyle como artefacto
                    archiveArtifacts artifacts: 'backend/target/checkstyle-result.xml', allowEmptyArchive: true
                }
            }
        }

        // =====================================================================
        // ESTRATEGIA 4: Paralelismo (segunda ronda)
        // Los builds de Backend y Frontend se ejecutan EN PARALELO.
        // =====================================================================
        stage('Build - Paralelo') {
            parallel {

                // --- BACKEND: Build de la aplicación ---
                stage('Backend - Build') {
                    steps {
                        echo '>>> Compilando el Backend (Spring Boot)...'
                        script {
                            runMaven('clean package -DskipTests')
                        }
                    }
                    post {
                        success {
                            // Archivar el JAR generado como artefacto
                            archiveArtifacts artifacts: 'backend/target/*.jar',
                                             fingerprint: true
                        }
                    }
                }

                // --- FRONTEND: Build de producción ---
                stage('Frontend - Build') {
                    steps {
                        echo '>>> Compilando el Frontend (Angular - producción)...'
                        script {
                            runNpm('run build -- --configuration=production')
                        }
                    }
                    post {
                        success {
                            // Archivar los archivos generados del frontend
                            archiveArtifacts artifacts: 'frontend/dist/**/*',
                                             fingerprint: true
                        }
                    }
                }
            }
        }

        // --- ETAPA 5: Build de imágenes Docker (solo en main/develop) ---
        stage('Docker Build') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                }
            }
            // ESTRATEGIA 4: Paralelismo - builds Docker en paralelo
            parallel {
                stage('Docker: Backend') {
                    steps {
                        echo '>>> Construyendo imagen Docker del Backend...'
                        dir('backend') {
                            sh 'docker build -t picoyplaca-backend:latest . || echo "Docker no disponible - omitiendo build de imagen"'
                        }
                    }
                }
                stage('Docker: Frontend') {
                    steps {
                        echo '>>> Construyendo imagen Docker del Frontend...'
                        dir('frontend') {
                            sh 'docker build -t picoyplaca-frontend:latest . || echo "Docker no disponible - omitiendo build de imagen"'
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // Post-acciones: se ejecutan al finalizar el pipeline
    // =========================================================================
    post {
        success {
            echo """
            =============================================
            Pipeline EXITOSO
            Rama: ${env.BRANCH_NAME}
            Build: #${env.BUILD_NUMBER}
            =============================================
            """
        }
        failure {
            echo """
            =============================================
            Pipeline FALLIDO
            Rama: ${env.BRANCH_NAME}
            Build: #${env.BUILD_NUMBER}
            Revisa los logs para más detalles.
            =============================================
            """
        }
        always {
            // Limpiar el workspace para no acumular archivos entre builds
            cleanWs()
        }
    }
}