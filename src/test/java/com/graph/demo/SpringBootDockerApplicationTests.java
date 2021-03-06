package com.graph.demo;

import org.apache.commons.configuration.MapConfiguration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.janusgraph.core.*;
import org.janusgraph.core.schema.JanusGraphIndex;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.core.schema.SchemaAction;
import org.janusgraph.core.schema.SchemaStatus;
import org.janusgraph.diskstorage.BackendException;
import org.janusgraph.example.GraphOfTheGodsFactory;
import org.janusgraph.graphdb.database.management.GraphIndexStatusReport;
import org.janusgraph.graphdb.database.management.ManagementSystem;
import org.janusgraph.hadoop.MapReduceIndexManagement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.apache.tinkerpop.gremlin.structure.T.id;
import static org.apache.tinkerpop.gremlin.structure.T.label;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class SpringBootDockerApplicationTests {
	protected static final String CONF_FILE = "resouces/jgex-tinkergraph.properties";

	@Test
	public void contextLoads() {
		System.out.println("ddd");
	}

	@Test
	public void mapReduceIndex() throws BackendException, ExecutionException, InterruptedException {
		//打开JanusGraph图，其配置存储在可访问的属性配置文件中
		//JanusGraphFactory是一个类，它通过在每次访问图形时提供一个配置对象来提供对图形的访问点。
		//ConfiguredGraphFactory提供了对您之前使用ConfigurationManagementGraph为其创建配置的图形的访问点。它还提供了一个访问点来管理图形配置。
		//ConfigurationManagementGraph允许您管理图形配置。
		//JanusGraphManager是一个跟踪图形引用的内部服务器组件，前提是您的图形被配置为使用它。

		//然而，这两个图形工厂之间有一个重要的区别:
		//1、只有将服务器配置为在服务器启动时使用ConfigurationManagementGraph api，才可以使用ConfiguredGraphFactory。
		//使用ConfiguredGraphFactory的好处是:
		//1、每次打开图形时，您只需要提供一个字符串来访问图形，而不是JanusGraphFactory——它要求您指定访问图形时希望使用的后端信息。
		//2、如果配置了分布式存储后端，那么集群中的所有JanusGraph节点都可以使用您的图形配置。

		JanusGraph graph = JanusGraphFactory
				.open("D:\\space\\spring_boot_graph\\src\\main\\resources\\jgex-cassandra.properties");
//		JanusGraph graph = JanusGraphFactory
//				.open("E:\\testspace\\spring_boot_docker\\src\\main\\resources\\jgex-cassandra.properties");
		// 遍历
		GraphTraversalSource g = graph.traversal();
		//定义属性
		JanusGraphManagement mgmt  = graph.openManagement();
		PropertyKey desc = mgmt.makePropertyKey("desc").dataType(String.class).make();
		mgmt.commit();

		//插入数据
		graph.addVertex("desc", "foo bar");
		graph.addVertex("desc", "foo baz");
		graph.tx().commit();

		// Create an index
		mgmt = graph.openManagement();

		PropertyKey desc1 = mgmt.getPropertyKey("name");
		JanusGraphIndex janusGraphIndex = mgmt.buildIndex("mixedExample", Vertex.class)
				.addKey(desc1).buildMixedIndex("search");
		mgmt.commit();
		//在定义索引之前， 回滚或提交事务
		graph.tx().rollback();
		GraphIndexStatusReport report  = ManagementSystem.awaitGraphIndexStatus(graph, "mixedExample").call();

		// 运行 janugraph-hadoop 重新构建索引
		mgmt = graph.openManagement();
		MapReduceIndexManagement mr1 = new MapReduceIndexManagement(graph);
		mr1.updateIndex(mgmt.getGraphIndex("mixedExample"), SchemaAction.REINDEX).get();

		// 开启索引
		mgmt = graph.openManagement();
		mgmt.updateIndex(mgmt.getGraphIndex("mixedExample"), SchemaAction.ENABLE_INDEX).get();
		mgmt.commit();

		mgmt = graph.openManagement();
		GraphIndexStatusReport report1 = ManagementSystem.
				awaitGraphIndexStatus(graph, "mixedExample").status(SchemaStatus.ENABLED).call();
		mgmt.rollback();
		// 排除janusgraph最后一个是查询缓存，索引重启一个实例
		graph.close();
		JanusGraphFactory
				.open("D:\\space\\spring_boot_docker\\src\\main\\resources\\jgex-cassandra.properties");
		g.V().has("desc","baz");


//		MapReduceIndexManagement mr = new MapReduceIndexManagement(graph);
//		ScanMetrics scanMetrics = mr.updateIndex(mgmt.getRelationIndex
//						(mgmt.getRelationType("battled"), "battlesByTime"),
//				SchemaAction.REINDEX).get();
//		mgmt.commit();

	}

	@Test
	public void sparkDemo(){
//		JanusGraph graph = JanusGraphFactory
//				.open("D:\\space\\spring_boot_graph\\src\\main\\resources\\spark-conf.properties");
//		GraphTraversalSource g = graph.traversal().withComputer(SparkGraphComputer.class);
//		// 3. Run some OLAP traversals
//        g.V().count();
//        g.E().count();


		//用内置的TinkerGraph-db创建一个空的图对象
		Graph g = TinkerGraph.open();
        //2.给图添加两个person顶点,按K-V对传入
		Vertex p1 = g.addVertex(T.label, "person", T.id, 1, "name", "jin");
		Vertex p2 = g.addVertex(T.label, "person", T.id, 2, "name", "tom");
        //3.给这两个点加一条边
		p1.addEdge("likes",p2, T.id, 3, "date", "20190220");


//		GraphTraversalSource t = TinkerGraph.open().traversal();
		//#新增顶点. 注意这里有一个内置的顶点id属性--> id==T.id  (其他图实例一般都是自动生成id)
		//#源码在org.apache.tinkerpop.gremlin.structure.T中,T是一个枚举(enum)
		//#T有:label,id,key,value四个儿子. 默认gremlin静态导入了T,所以你可以直接使用它的儿子
//		 Vertex v1 = t.addV("person").property(id, 1)
//				.property("pid", "0x01")
//				.property("name", "A")
//				.property("age", 18).next();//next()去了不报错.但是后面添加边关联的时候就会报错. 作用待确认
//
//		Vertex v2 = t.addV("software").property(id, 3)
//				.property("pid", "0x02")
//				.property("name", "janus")
//				.property("lang", "go").next();


	}


	@Test
	public void searGraph(){
		/*
		Cassandra可以嵌入到JanusGraph中，这意味着JanusGraph和Cassandra在同一个JVM中运行并通过进程调用而不是通过网络进行通信。
		这消除了（反）序列化和网络协议开销，因此可以带来显着的性能改进。
		在此部署模式下，JanusGraph在内部启动cassandra守护程序，而JanusGraph不再连接到现有集群，而是自己的集群。
		 */
        // 这个地址是公司电脑地址
		//通过配置文件构建图对象
		JanusGraph graph = JanusGraphFactory
				.open("D:\\space\\spring_boot_graph\\src\\main\\resources\\jgex-cql.properties");

		/*
		 *因为Janus存在Cassandra中的所有表,
		 * 所有列的类型都是blob(binary large object),也就是二进制对象.所有直接看都是奇怪的字节码.
		 * 那么我们如何测试Janus创建Schema是否成功,是否正确,数据解析是否OK呢?
		 * 我建议先从小数据测试开始,完整性看行数. 但是,我们如何查看具体的数据呢?
		 * 难道序列化之后的数据只能反序列化在Janus才能查看么? 可以通过RowID先简单看看.
		 */


		// 这个地址是家里电脑地址
//		JanusGraph graph = JanusGraphFactory
//				.open("E:\\testspace\\spring_boot_graph\\src\\main\\resources\\jgex-cql.properties");

//		GraphOfTheGodsFactory.load(graph);

//		如果用于打开图形的配置指定graph.graphname但未指定后端的存储目录，
//       tablename或keyspacename，则相关参数将自动设置为值graph.graphname。
//       但是，如果您提供其中一个参数，则该值始终优先。如果您不提供，则默认为配置选项的默认值

		//Create Schema
//		JanusGraphManagement mgmt  = graph.openManagement();
//		//设置顶点标签属性, 顶点
//		VertexLabel student = mgmt.makeVertexLabel("student").make();
//		//设置边标签属性
//		EdgeLabel friends = mgmt.makeEdgeLabel("friends").make();
//		mgmt.commit();
//		//使用label student 创建一个顶点v1,并向顶点添加属性
//		JanusGraphVertex v1 = graph.addVertex(label, "student");
//	    v1.property("id", "1");
//
//		JanusGraphVertex v2 = graph.addVertex();
//	    JanusGraphVertex v3 = graph.addVertex(label, "student");
//	    v3.property("id", "2");
//	    graph.tx().commit();
//		v1.addEdge("friends", v2);
//      v1.addEdge("friends", v3);
//      graph.tx().commit();
//
//		System.out.println(graph.traversal().V());
//		System.out.println(graph.traversal().V().values("id"));
//		System.out.println(graph.traversal().E());


// -----------------------------------------------------------------
		// 下面 的方式创建 有错
//		Map<String,String> map = new HashMap<>();
//		map.put("storage.backend", "cql");
//		map.put("storage.hostname", "192.168.199.117");
//		ConfiguredGraphFactory.createTemplateConfiguration(new MapConfiguration(map));
//
//		JanusGraph graph1 = ConfiguredGraphFactory.create("graph1");
//		JanusGraphManagement mgmt  = graph1.openManagement();
////		//设置顶点标签属性, 顶点
//		VertexLabel student = mgmt.makeVertexLabel("student").make();
//		//设置边标签属性
//		EdgeLabel friends = mgmt.makeEdgeLabel("friends").make();
//		mgmt.commit();
//		//使用label student 创建一个顶点v1,并向顶点添加属性
//		JanusGraphVertex v1 = graph1.addVertex(label, "student");
//	    v1.property("id", "1");
//
//		JanusGraphVertex v2 = graph1.addVertex();
//	    JanusGraphVertex v3 = graph1.addVertex(label, "student");
//	    v3.property("id", "2");
//		graph1.tx().commit();
//		v1.addEdge("friends", v2);
//        v1.addEdge("friends", v3);
//		graph1.tx().commit();
//
//		System.out.println(graph1.traversal().V());
//		System.out.println(graph1.traversal().V().values("id"));
//		System.out.println(graph1.traversal().E());

// ----------------------------------------------------------------------


		// traversal 遍历
		GraphTraversalSource g = graph.traversal().withComputer(SparkGraphComputer.class);
//		GraphTraversal<Vertex, Long> count = g.V().count();
//		GraphTraversal<Edge, Long> count1 = g.E().count();


		System.out.println("查询顶点====="+g.V());
		System.out.println("查询顶点label====="+g.V().hasLabel("titan")); // 查询label为‘knows’的边。

		GraphTraversal<Vertex, Vertex> has1 = g.V().has("name", "titan");
		System.out.println(has1);
		GraphTraversal<Vertex, Vertex> out = g.V().has("name", "location");
		System.out.println(out);

		System.out.println("查询边====="+g.E().has("name","father"));


		// (Step代表每次遍历的一个步骤)
//		Step在图服务中一般指的是遍历中的计算最小单元(而非生活中常说的步子) ,
		// 更好理解的解释是步骤, 每个Step接受一个元素对象作为入参, 加工处理后返回.
		// 在一次Traversal中, 会有很多个步骤(Step), 它们就以流的方式加载, 并且产生一个延迟的加工计算链 (流式编程的显著优点之一)
//
//		而Path表示一次遍历(Traversal)中的某一条路径选择, 这个应该是最贴合我们日常说的”路径”的,
		// 任何实现的XxxPath都有两个list : 一个是这条路径经过所包含的元素,另一个记录这些元素的标签名(比如person/age/name..) ,
		// Path因为代表的是某条路径, 所以使用上比Step少很多

		// 遍历结果 [GraphStep(vertex,[]), HasStep([friends.eq(id)])]



// 官网 示例 添加，这个指定了keyspacename
//		map = new HashMap();
//		map.put("storage.backend", "cql");
//		map.put("storage.hostname", "127.0.0.1");
//		ConfiguredGraphFactory.createTemplateConfiguration(new
//				MapConfiguration(map));
//
//		g1 = ConfiguredGraphFactory.create("graph1"); //keyspace === graph1
//		g2 = ConfiguredGraphFactory.create("graph2"); //keyspace === graph2
//		g3 = ConfiguredGraphFactory.create("graph3"); //keyspace === graph3
	}


	@Test
	public  void VisitJanusGraph(){
		//First configure the graph
//		JanusGraphFactory.Builder builder = JanusGraphFactory.build();
		JanusGraph graph = JanusGraphFactory
				.open("D:\\space\\spring_boot_graph\\src\\main\\resources\\jgex-cql.properties");

//		builder.set("storage.backend", "cassandra");
//		builder.set("storage.hostname", "192.168.199.117");
//		builder.set("storage.cql.keyspace","test");
//		builder.set("storage.port", "9042");
		//ip address where cassandra is installed
		//builder.set("storage.username", “cassandra”);
		//buder.set("storage.password", “cassandra”);
		//builder.set("storage.cassandra.keyspace", "testing");
//		JanusGraphManagement management = graph.openManagement();
//		management.makeVertexLabel("person").make();
//		management.commit();
		//open a graph database
//		JanusGraph graph = builder.open();
		//Open a transaction
		JanusGraphTransaction tx = graph.newTransaction();
		//Create a vertex v1 with label student, add property to the vertex
		Vertex v1 = tx.addVertex(T.label, "location","name","张三");
		//create a vertex v2 without label and property
		Vertex v2 = tx.addVertex(T.label,"monster","name","李四");
		//create a vertex v3 with label student, then add property to the vertex
		Vertex v3 = tx.addVertex(T.label, "god","name","小红");
		tx.commit();



		//Create edge between 2 vertices
		Edge edge12 = v1.addEdge("sister", v2);
		Edge edge13 = v1.addEdge("friends", v3);
		//Finally commit the transaction
		tx.commit();

		System.out.println(graph.traversal().V());
		System.out.println(graph.traversal().E());
	}


	@Test
	public void  te(){

		/*
		 * JanusGraph是一个图形数据库引擎。JanusGraph本身专注于紧凑图形序列化，
		 * 丰富的图形数据建模和高效的查询执行。此外，JanusGraph利用Hadoop进行图形分析和批处理图处理。JanusGraph为数据持久性，
		 * 数据索引和客户端访问实现了强大的模块化接口。JanusGraph的模块化架构使其能够与各种存储
		 * ，索引和客户端技术进行互操作; 它还简化了扩展JanusGraph以支持新的过程。
		 */
		/*
		使用配置文件来连接到相应服务器上的存储数据库
		 */
		JanusGraph graph = JanusGraphFactory
				.open("D:\\space\\spring_boot_graph\\src\\main\\resources\\jgex-cql.properties");
		//Create Schema
		JanusGraphManagement mgmt  = graph.openManagement();

		//创建了一个名字为name的属性，并设置值类型为LONG，且只能保存一个值
		//使用cardinality(Cardinality)定义与在任何给定的顶点的键关联的值允许的基数。


		/*
		 SINGLE：对于此类密钥，每个元素最多允许一个值。换句话说，键→值映射对于图中的所有元素都是唯一的。
		 属性键birthDate是具有SINGLE基数的示例，因为每个人只有一个出生日期。
		 创建了一个名字为birthDate的属性，并设置值类型为LONG，且只能保存一个值
		 */
		PropertyKey birthDate  =  mgmt.makePropertyKey("birthDate").dataType(Long.class).cardinality(Cardinality.SINGLE).make();
		/*
		  SET：允许多个值，但每个元素没有重复值用于此类键。换句话说，密钥与一组值相关联。
		  name如果我们想要捕获个人的所有姓名（包括昵称，婚前姓名等），则属性键具有SET基数。
		  创建了一个名字为name的属性，并设置值类型为String，且可以保存不能重复的多个值
		 */
		final PropertyKey name = mgmt.makePropertyKey("name").dataType(String.class).cardinality(Cardinality.SET).make();
		/*
		 LIST：允许每个元素的任意数量的值用于此类键。换句话说，密钥与允许重复值的值列表相关联。
		 假设我们将传感器建模为图形中的顶点，则属性键sensorReading是具有LIST基数的示例，以允许记录大量（可能重复的）传感器读数。
		 创建了一个名字为sensorReading的属性，并设置值类型为Double，且可以保存可以重复的多个值
		 */
		PropertyKey sensorReading = mgmt.makePropertyKey("sensorReading").dataType(Double.class).cardinality(Cardinality.LIST).make();

	}


	@Test
	public void build(){
		//使用配置文件来连接到相应服务器上的存储数据库
		JanusGraph graph = JanusGraphFactory
				.open("D:\\space\\spring_boot_graph\\src\\main\\resources\\jgex-cql.properties");
		//Create Schema
		JanusGraphManagement mgmt  = graph.openManagement();

		/**
		*t1_crm_customer_open_card.信用卡卡号      客户开卡表
		*t1_crm_customer_bill.信用卡卡号           信用卡卡号
		*t1_crm_stop_elec.信用卡卡号               停止电催表
		*t1_crm_commission_case.信用卡卡号         委案表
		*t1_crm_ovedue.信用卡卡号                  逾期表
		*t1_crm_natural_payoff.信用卡卡号          自然结清表
		*t1_crm_legal_support.信用卡卡号           法务支持
		*t1_crm_tm_loan.信用卡卡号                 腾铭贷款合同
		*t1_crm_elec_payment_register.信用卡卡号   电催回款登记
		*/

		/**
		 * 定义银行顶点属性
		 */
		VertexLabel bankCard = mgmt.makeVertexLabel("bank_card").make();
		/**
		 * 设置银行卡属性 卡号
		 */
		PropertyKey credit_card_number = mgmt.makePropertyKey("credit_card_number").dataType(String.class).cardinality(Cardinality.SINGLE).make();
		/**
		 *开类型
		 */
		PropertyKey bankCardTye = mgmt.makePropertyKey("bank_card_type").dataType(String.class).cardinality(Cardinality.SINGLE).make();
		/**
		 * 设置银行卡属性 编码
		 */
		PropertyKey bankCode = mgmt.makePropertyKey("bank_code").dataType(String.class).cardinality(Cardinality.SINGLE).make();
		/**
		 * 信用卡卡表上的营销代码   t1_oa_icbc_base_info
		 */
		PropertyKey dscode = mgmt.makePropertyKey("dscode").dataType(String.class).cardinality(Cardinality.SINGLE).make();

		/**
		 *
		 * t1_crm_ovedue.银行预留电话 逾期表
		 */
		PropertyKey bank_reserve_phone = mgmt.makePropertyKey("bank_reserve_phone").dataType(String.class).cardinality(Cardinality.SINGLE).make();


		/**
		 * 设置银行卡边. 手机
		 * 属性紧急联系人手机号
		 */
		EdgeLabel e_contact_phone = mgmt.makeEdgeLabel("e_contact_phone").multiplicity(Multiplicity.MULTI).make();
		/**
		 * 联系电话
		 * t1_oa_loan_requests.联系人电话1       贷款申请表
		 * t1_oa_loan_requests.联系电话2
		 */
		EdgeLabel contact_phone = mgmt.makeEdgeLabel("contact_phone").multiplicity(Multiplicity.MULTI).make();

		/**
		 * 银行卡相关边 （公司、单位）
		 * 打款账号
		 * t1_crm_no_repay_manage.客户打款账号  未回款管理表
		 */
		EdgeLabel pay_account = mgmt.makeEdgeLabel("pay_account").multiplicity(Multiplicity.MULTI).make();
		/**
		 * t1_crm_elec_payment_register.打款说明    电催回款登记表
		 */
		EdgeLabel payment_instructions = mgmt.makeEdgeLabel("payment_instructions").multiplicity(Multiplicity.MULTI).make();
		/**
		 * t1_crm_payoff.邮寄地址                   结清表
		 */
		EdgeLabel express_address = mgmt.makeEdgeLabel("express_address").multiplicity(Multiplicity.MULTI).make();




		/*
		t1_crm_tm_loan.主贷人手机号码1                腾铭贷款合同
		t1_crm_tm_loan.主贷人手机号码2
		t1_crm_tm_loan.业务员手机号码
		t1_crm_tm_loan.紧急联系人手机号码
		t1_crm_tm_loan.原主贷人手机号码
		t1_crm_tm_loan.配偶共同还款人手机号码
		t1_crm_tm_loan.担保人手机号码
		t1_crm_tm_loan.紧急联系人2手机号码

		t1_crm_ecel_manage.手机号码1                  电催管理
		t1_crm_ecel_manage.手机号码2
		t1_crm_ecel_manage.紧急联系人手机号码
        t1_crm_ecel_manage.原主贷人手机号码
        t1_crm_ecel_manage.配偶共同还款人手机号码
        t1_crm_ecel_manage.担保人手机号码
        t1_crm_ecel_manage.紧急联系人2手机号码

        t1_crm_payoff.领取人收件人手机号码                结清表
		t1_crm_payoff.主贷人手机号码
		t1_crm_payoff.主贷人手机号码2

		t1_crm_customer_loan.业务员手机号码              客户贷款表

		t1_crm_complain_manage.手机号码1                投诉处理
		t1_crm_complain_manage.手机号码2
		t1_crm_complain_manage.紧急联系人手机号码
		t1_crm_complain_manage.原主贷人手机号码
		t1_crm_complain_manage.配偶共同还款人手机号码
		t1_crm_complain_manage.担保人手机号码
		t1_crm_complain_manage.紧急联系人2手机号码

		t1_crm_no_repay_manage.主贷人手机号码1            未回款管理
		t1_crm_no_repay_manage.主贷人手机号码2
        t1_crm_no_repay_manage.业务员手机号码
		t1_crm_no_repay_manage.原主贷人手机号码
		t1_crm_no_repay_manage.配偶共同还款人手机号码
		t1_crm_no_repay_manage.担保人手机号码
		t1_crm_no_repay_manage.紧急联系人1手机号码
		t1_crm_no_repay_manage.紧急联系人2手机号码


		t1_crm_natural_payoff.领取人收件人手机号码          自然结清表
		t1_crm_natural_payoff.主贷人手机号码
		t1_crm_natural_payoff.主贷人手机号码2

        t1_crm_gps_monitoring.主贷人手机号码1              GPS日常监控
		t1_crm_gps_monitoring.主贷人手机号码2

		t1_oa_loan_requests.担保人手机号                   贷款申请

		t1_oa_user.mobile                               【新】用户表

		t1_oa_score_client.mobile                        FICO征信表
		*/

		/**
		 * 定义手机顶点
		 */
		VertexLabel mobile_phone = mgmt.makeVertexLabel("mobile_phone").make();
		/**
		 * 定义顶点属性 手机号
		 */
		PropertyKey phone_number = mgmt.makePropertyKey("phone_number").dataType(Integer.class).cardinality(Cardinality.SET).make();
		/**
		 * 手机设备token
		 * t1_oa_user.device_token  新用户表
		 */
		PropertyKey device_token = mgmt.makePropertyKey("device_token").dataType(String.class).cardinality(Cardinality.SINGLE).make();
		/**
		 * 手机设备系统
		 * t1_oa_user.device_os
		 */
		PropertyKey device_os = mgmt.makePropertyKey("device_os").dataType(String.class).cardinality(Cardinality.SINGLE).make();
		/**
		 * 手机设备系统版本
		 * t1_oa_user.device_os_version
		 */
		PropertyKey device_os_version = mgmt.makePropertyKey("device_os_version").dataType(String.class).cardinality(Cardinality.SINGLE).make();
		//运营商 、IMEI2 、IMEI1


		/**
		 * 定义手机相关边属性
		 * t1_crm_tm_loan.紧急联系人手机号码
		 */
		EdgeLabel e_contact_phone1 = mgmt.makeEdgeLabel("e_contact_phone").multiplicity(Multiplicity.MULTI).make();
		/**
		 * t1_crm_tm_loan.紧急联系人2手机号码
		 */
		EdgeLabel e_contact_phone2 = mgmt.makeEdgeLabel("e_contact_phone2").multiplicity(Multiplicity.MULTI).make();
		/**
		 * t1_crm_payoff.主贷人手机号码1
		 */
		EdgeLabel primary_lender_phone1 = mgmt.makeEdgeLabel("primary_lender_phone1").multiplicity(Multiplicity.MULTI).make();
		/**
		 * t1_crm_payoff.主贷人手机号2
		 */
		EdgeLabel primary_lender_phone2 = mgmt.makeEdgeLabel("primary_lender_phone2").multiplicity(Multiplicity.MULTI).make();
		/**
		 * t1_crm_customer_loan.业务员手机号码
		 */
		EdgeLabel salesman_phone = mgmt.makeEdgeLabel("salesman_phone").multiplicity(Multiplicity.MULTI).make();
		/**
		 * t1_crm_tm_loan.原主贷人手机号码
		 */
		EdgeLabel o_lender_phone = mgmt.makeEdgeLabel("o_lender_phone").multiplicity(Multiplicity.MULTI).make();
		/**
		 * t1_crm_tm_loan.配偶共同还款人手机号码
		 */
		EdgeLabel spouse_common_phone = mgmt.makeEdgeLabel("spouse_common_phone").multiplicity(Multiplicity.MULTI).make();
		/**
		 * t1_crm_tm_loan.担保人手机号码
		 */
		EdgeLabel guarantor_phone = mgmt.makeEdgeLabel("guarantor_phone").multiplicity(Multiplicity.MULTI).make();
		/**
		 * t1_crm_payoff.领取人收件人手机号码
		 */
		EdgeLabel recipients_phone = mgmt.makeEdgeLabel("recipients_phone").multiplicity(Multiplicity.MULTI).make();
		/**
		 * t1_crm_complain_manage.主贷人单位电话
		 */
		EdgeLabel primary_lender_company_phone = mgmt.makeEdgeLabel("primary_lender_company_phone").multiplicity(Multiplicity.MULTI).make();



		/*
		t1_crm_tm_loan.主贷人身份证号码                           腾铭贷款合同表
		t1_crm_tm_loan.原主贷人身份证号码
		t1_crm_dy_elec_record.主贷人身份证号码                    迪扬电催记录表
		t1_crm_ecel_manage.身份证号码                            电催管理表
		t1_crm_ecel_manage.原主贷人身份证号码
		t1_crm_elec_payment_register.主贷人身份证号码             电催回款登记表
		t1_crm_home_visit_form.主贷人身份证号码                   家访单表
		t1_crm_payoff.主贷人身份证号码                            结清表
		t1_crm_customer_loan.主贷人身份证号码                     客户贷款表
		t1_crm_customer_open_card.主贷人身份证号码                客户开卡表
		t1_crm_recieve_car.主贷人身份证号码                       收车表
		t1_crm_stop_elec.主贷人身份证号码                         停止电催表
		t1_crm_complain_manage.主贷人身份证号码                   投诉处理
		t1_crm_complain_manage.原主贷人身份证号码
		t1_crm_commission_case.主贷人身份证号码                   委案表
		t1_crm_no_repay_manage.主贷人身份证号码                   未回款管理
		t1_crm_no_repay_manage.原主贷人身份证号码
		t1_crm_ovedue.主贷人身份证号码                            逾期表
		t1_crm_natural_payoff.主贷人身份证号码                    自然结清表
		t1_crm_dispatch_order.主贷人身份证号码                    派单信息表
		t1_crm_cancel_dispatch.主贷人身份证号码                   取消派单表
		t1_crm_return_dispatch.主贷人身份证号码                   派单退回表
		t1_crm_negotiation.主贷人身份证号码                       谈判表
		t1_crm_storage.主贷人身份证号码                           入库表
		t1_crm_out_storage.主贷人身份证号码                       出库单
		t1_crm_vehicle_disposal.主贷人身份证号码                  车辆处置
		t1_crm_legal_support.主贷人身份证号码                     法务支持
		t1_crm_gps_monitoring.主贷人身份证号码                    GPS日常监控

		t1_oa_loan_requests.guarantor_credential(担保人身份证)   贷款申请
		t1_oa_loan_requests.car_seller_credential(前车主身份证号)

		t1_oa_score_client.IDCARD                             FICO征信表
        */

		/**
		 * 定义证件顶点
		 */
		VertexLabel certificates = mgmt.makeVertexLabel("certificates").make();
		/**
		 * 证件类型 0:身份证 1:护照 2:军官证 3:士兵证 4:回乡证 5:临时身份证 6:户口本 7:其他 9:警官证
		 * 定义顶点属性
		 */
		/**
		 * t1_oa_icbc_base_info.证件号码   工行e分期基础信息表
		 */
		PropertyKey certificates_number = mgmt.makePropertyKey("certificates_number").dataType(String.class).cardinality(Cardinality.SINGLE).make();
		/**
		 * t1_oa_icbc_base_info.证件类型
		 */
		PropertyKey certificates_type = mgmt.makePropertyKey("certificates_type").dataType(String.class).cardinality(Cardinality.SINGLE).make();

		/**
		 * t1_oa_user.身份证有效期开始时间  新用户表
		 */
		PropertyKey credential_start_date = mgmt.makePropertyKey("credential_start_date").dataType(Long.class).cardinality(Cardinality.SINGLE).make();
		/**
		 * t1_oa_user.身份证过期时间
		 */
		PropertyKey credential_expire_date = mgmt.makePropertyKey("credential_expire_date").dataType(Long.class).cardinality(Cardinality.SINGLE).make();


		/**
		 *  定义边
		 *
		 * t1_crm_tm_loan.主贷人身份证号码     腾铭贷款合同
		 */
		EdgeLabel id_card = mgmt.makeEdgeLabel("id_card").multiplicity(Multiplicity.MULTI).make();
		/**
		 * t1_crm_recieve_car.驾驶证         收车表
		 */
		EdgeLabel driver_license = mgmt.makeEdgeLabel("driver_license").multiplicity(Multiplicity.MULTI).make();



	    /**
		定义建筑顶点
		 （公司/住宅 地址）
		 */
		VertexLabel building = mgmt.makeVertexLabel("building").make();
		/**
		 定义顶点属性
		 省
		 */
		PropertyKey name = mgmt.makePropertyKey("province").dataType(String.class).cardinality(Cardinality.SET).make();
		/**
		 * 城市
		 */
		PropertyKey address = mgmt.makePropertyKey("city").dataType(String.class).cardinality(Cardinality.SET).make();
		/**
		 * 地区
		 */
		PropertyKey area = mgmt.makePropertyKey("area").dataType(String.class).cardinality(Cardinality.SET).make();


		/**
		 *定义边
		 *t1_oa_company.address(公司地址)                收款公司（垫款公司主体）
		 */
		EdgeLabel company_address = mgmt.makeEdgeLabel("company_address").multiplicity(Multiplicity.MULTI).make();

		/**
		 * t1_oa_icbc_base_info.haddress(住宅地址)       工行e分期基础信息表
		 */
		EdgeLabel residential_address = mgmt.makeEdgeLabel("residential_address").multiplicity(Multiplicity.MULTI).make();


	}
}

