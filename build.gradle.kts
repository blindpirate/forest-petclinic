import com.github.spotbugs.snom.SpotBugsTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.3.72"
    id("com.github.spotbugs") version ("4.5.0") apply (false)
}

apply(from = "gradle/dependencies.gradle.kts")

val libs: (String) -> String by rootProject.ext

allprojects {
    repositories {
        jcenter()
        mavenCentral()
        maven { url = URI("https://oss.sonatype.org/content/repositories/snapshots") }
    }

    configureJava()

    if (file("src/main/kotlin").isDirectory || file("src/test/kotlin").isDirectory) {
        configureKotlin()
    }

    if (file("src/browserTest/groovy").isDirectory) {
        configureGroovy()
    }
}

fun Project.configureGroovy() {
    apply(plugin = "groovy")
    apply(plugin = "codenarc")
    sourceSets.create("browserTest") {
        withConvention(GroovySourceSet::class) {
            groovy.srcDir("src/core/groovy")
        }
        resources.srcDir("src/browserTest/resources")
    }
    dependencies {
        "browserTestImplementation"(sourceSets["main"].output)
        "browserTestImplementation"(sourceSets["test"].output)
        "browserTestImplementation"(configurations["testImplementation"])
    }

    tasks.register("browserTest", Test::class) {
        testClassesDirs = sourceSets["browserTest"].output.classesDirs
        classpath = sourceSets["browserTest"].runtimeClasspath
        useJUnitPlatform()
        mustRunAfter("test")
    }
    tasks.check.configure { dependsOn(tasks.withType<Test>()) }
}

fun Project.configureKotlin() {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    configureKtlint()

    tasks.withType<KotlinCompile>() {
        kotlinOptions.jvmTarget = "1.8"
    }

    dependencies {
        testImplementation(libs("kotlinx-coroutines-jdk8"))
        testImplementation(libs("kotlinx-coroutines-core"))
        testImplementation(libs("kotlin-stdlib-jdk8"))
    }
}

fun Project.configureJava() {
    /*
    java.lang.instrument.IllegalClassFormatException: Error while instrumenting io/forestframework/ext/core/RouterWithPredefinedRootMethodAccess.
	at org.jacoco.agent.rt.internal_43f5073.CoverageTransformer.transform(CoverageTransformer.java:94)
	at java.instrument/java.lang.instrument.ClassFileTransformer.transform(ClassFileTransformer.java:246)
	at java.instrument/sun.instrument.TransformerManager.transform(TransformerManager.java:188)
	at java.instrument/sun.instrument.InstrumentationImpl.transform(InstrumentationImpl.java:563)
	at java.base/java.lang.ClassLoader.defineClass1(Native Method)
	at java.base/java.lang.ClassLoader.defineClass(ClassLoader.java:1017)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:566)
	at com.esotericsoftware.reflectasm.AccessClassLoader.defineClass(AccessClassLoader.java:73)
	at com.esotericsoftware.reflectasm.AccessClassLoader.defineAccessClass(AccessClassLoader.java:57)
	at com.esotericsoftware.reflectasm.MethodAccess.get(MethodAccess.java:276)
	at io.forestframework.utils.ReflectionUtils$InvocationContext.<init>(ReflectionUtils.java:21)
	at io.forestframework.utils.ReflectionUtils$InvocationContext.<init>(ReflectionUtils.java:16)
	at io.forestframework.utils.ReflectionUtils.lambda$invoke$0(ReflectionUtils.java:13)
	at java.base/java.util.concurrent.ConcurrentHashMap.computeIfAbsent(ConcurrentHashMap.java:1705)
	at io.forestframework.utils.ReflectionUtils.invoke(ReflectionUtils.java:13)
	at io.forestframework.core.http.AbstractWebRequestHandler.invokeViaJavaReflection(AbstractWebRequestHandler.java:63)
	at io.forestframework.core.http.AbstractWebRequestHandler.invokeMethod(AbstractWebRequestHandler.java:57)
	at io.forestframework.core.http.AbstractWebRequestHandler.lambda$invokeRouting$0(AbstractWebRequestHandler.java:36)
	at java.base/java.util.concurrent.CompletableFuture.uniComposeStage(CompletableFuture.java:1106)
	at java.base/java.util.concurrent.CompletableFuture.thenCompose(CompletableFuture.java:2235)
	at io.forestframework.core.http.AbstractWebRequestHandler.invokeRouting(AbstractWebRequestHandler.java:36)
	at io.forestframework.core.http.DefaultPlainHttpRequestHandler.lambda$invokeHandlers$16(DefaultPlainHttpRequestHandler.java:194)
	at java.base/java.util.concurrent.CompletableFuture.uniComposeStage(CompletableFuture.java:1106)
	at java.base/java.util.concurrent.CompletableFuture.thenCompose(CompletableFuture.java:2235)
	at io.forestframework.core.http.DefaultPlainHttpRequestHandler.invokeHandlers(DefaultPlainHttpRequestHandler.java:194)
	at io.forestframework.core.http.DefaultPlainHttpRequestHandler.lambda$handle$5(DefaultPlainHttpRequestHandler.java:58)
	at io.forestframework.core.http.DefaultPlainHttpRequestHandler.lambda$composeSafely$9(DefaultPlainHttpRequestHandler.java:95)
	at java.base/java.util.concurrent.CompletableFuture.uniComposeStage(CompletableFuture.java:1106)
	at java.base/java.util.concurrent.CompletableFuture.thenCompose(CompletableFuture.java:2235)
	at io.forestframework.core.http.DefaultPlainHttpRequestHandler.composeSafely(DefaultPlainHttpRequestHandler.java:93)
	at io.forestframework.core.http.DefaultPlainHttpRequestHandler.handle(DefaultPlainHttpRequestHandler.java:46)
	at io.forestframework.core.http.routing.PlainHttpRoutingMatchResult.select(PlainHttpRoutingMatchResult.java:40)
	at io.forestframework.core.http.DefaultHttpRequestDispatcher.handle(DefaultHttpRequestDispatcher.java:47)
	at io.forestframework.core.http.DefaultHttpRequestDispatcher.handle(DefaultHttpRequestDispatcher.java:25)
	at io.vertx.core.http.impl.WebSocketRequestHandler.handle(WebSocketRequestHandler.java:53)
	at io.vertx.core.http.impl.WebSocketRequestHandler.handle(WebSocketRequestHandler.java:31)
	at io.vertx.core.http.impl.Http1xServerConnection.lambda$handleMessage$0(Http1xServerConnection.java:133)
	at io.vertx.core.impl.AbstractContext.emit(AbstractContext.java:181)
	at io.vertx.core.impl.AbstractContext.lambda$dispatch$0(AbstractContext.java:84)
	at io.vertx.core.impl.EventLoopContext.schedule(EventLoopContext.java:59)
	at io.vertx.core.impl.EventLoopContext$Duplicated.schedule(EventLoopContext.java:131)
	at io.vertx.core.impl.AbstractContext.schedule(AbstractContext.java:94)
	at io.vertx.core.impl.AbstractContext.dispatch(AbstractContext.java:84)
	at io.vertx.core.http.impl.Http1xServerConnection.handleMessage(Http1xServerConnection.java:131)
	at io.vertx.core.net.impl.ConnectionBase.read(ConnectionBase.java:127)
	at io.vertx.core.net.impl.VertxHandler.channelRead(VertxHandler.java:146)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
	at io.netty.channel.ChannelInboundHandlerAdapter.channelRead(ChannelInboundHandlerAdapter.java:93)
	at io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtensionHandler.channelRead(WebSocketServerExtensionHandler.java:101)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
	at io.vertx.core.http.impl.Http1xUpgradeToH2CHandler.channelRead(Http1xUpgradeToH2CHandler.java:115)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
	at io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:324)
	at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:296)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
	at io.vertx.core.http.impl.Http1xOrH2CHandler.end(Http1xOrH2CHandler.java:61)
	at io.vertx.core.http.impl.Http1xOrH2CHandler.channelRead(Http1xOrH2CHandler.java:38)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:357)
	at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1410)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:379)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:365)
	at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:919)
	at io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:163)
	at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:714)
	at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:650)
	at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:576)
	at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:493)
	at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:989)
	at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
	at java.base/java.lang.Thread.run(Thread.java:834)
Caused by: java.io.IOException: Error while instrumenting io/forestframework/ext/core/RouterWithPredefinedRootMethodAccess.
	at org.jacoco.agent.rt.internal_43f5073.core.instr.Instrumenter.instrumentError(Instrumenter.java:159)
	at org.jacoco.agent.rt.internal_43f5073.core.instr.Instrumenter.instrument(Instrumenter.java:109)
	at org.jacoco.agent.rt.internal_43f5073.CoverageTransformer.transform(CoverageTransformer.java:92)
	... 83 more
Caused by: java.lang.IllegalArgumentException
	at org.jacoco.agent.rt.internal_43f5073.asm.ClassReader.readVerificationTypeInfo(ClassReader.java:3178)
	at org.jacoco.agent.rt.internal_43f5073.asm.ClassReader.readStackMapFrame(ClassReader.java:3105)
	at org.jacoco.agent.rt.internal_43f5073.asm.ClassReader.readCode(ClassReader.java:1855)
	at org.jacoco.agent.rt.internal_43f5073.asm.ClassReader.readMethod(ClassReader.java:1284)
	at org.jacoco.agent.rt.internal_43f5073.asm.ClassReader.accept(ClassReader.java:688)
	at org.jacoco.agent.rt.internal_43f5073.asm.ClassReader.accept(ClassReader.java:400)
	at org.jacoco.agent.rt.internal_43f5073.core.instr.Instrumenter.instrument(Instrumenter.java:89)
	at org.jacoco.agent.rt.internal_43f5073.core.instr.Instrumenter.instrument(Instrumenter.java:107)
	... 84 more
     */
//    apply(plugin = "jacoco")
    apply(plugin = "java-library")
    apply(plugin = "checkstyle")
    apply(plugin = "com.github.spotbugs")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    tasks.test {
        useJUnitPlatform()
    }

    tasks.named("check").configure {
        dependsOn("spotbugsMain")
    }

    tasks.withType<SpotBugsTask>().configureEach {
        isEnabled = name == "spotbugsMain" && !project.file("src/main/kotlin").isDirectory
        reports.maybeCreate("xml").isEnabled = false
        reports.maybeCreate("html").isEnabled = true
    }
}

fun Project.configureKtlint() {
    configurations.create("ktlint")
    dependencies {
        val ktlintVersion = "0.36.0"
        "ktlint"("com.pinterest:ktlint:$ktlintVersion")
    }

    val ktlintTask = tasks.register<JavaExec>("ktlint") {
        group = "verification"
        description = "Check Kotlin code style"
        classpath = configurations["ktlint"]
        main = "com.pinterest.ktlint.Main"
        args("src/main/**/*.kt", "src/test/**/*.kt")
    }

    tasks.named("check").configure { dependsOn(ktlintTask) }

    tasks.register<JavaExec>("ktlintFormat") {
        group = "formatting"
        description = "Fix Kotlin code style deviations"
        classpath = configurations["ktlint"]
        main = "com.pinterest.ktlint.Main"
        args("-F", "src/main/**/*.kt", "src/test/**/*.kt")
    }
}


extensions.findByName("buildScan")?.withGroovyBuilder {
    setProperty("termsOfServiceUrl", "https://gradle.com/terms-of-service")
    setProperty("termsOfServiceAgree", "yes")
}