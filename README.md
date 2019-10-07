# Play-Java sbt jOOQ seed

### How to run
1. The project is configured for MySQL. So change your driver and DB username + password in `/conf/jooq-codegen.xml`. 
The xml tags `jdbc.url` and `generator.database.inputSchema` also need to be changed.
2. Also change `application.conf` accordingly.
3. Change database dialect in `PersonRepo.java` - so you can use your database specific features.
3. Run the sql scripts to generate database schema (skip this if you already have a database).
4. Run the task `jooqCodegen` which would read the schema and generate 
classes corresponding to the database tables in the folder 
`/target/scala-2.13/src_managed/main`.
5. Now the project should compile properly with these jOOQ generated classes.

### Upgrade

* To upgrade jOOQ version or JDBC Driver, change values in `/project/plugins.sbt`. 
Settings in `/build.sbt` would use the same values automatically.
* If you rename or change location of `jooq-codegen.xml` you need to update the 
SBT setting `jooqConfigFile`.

### Motivation
There already exists an excellent plugin [sbt-jooq](https://github.com/kxbmap/sbt-jooq/)
which should be the go-to solution for jOOQ integration. It covers a lot of use cases:
* works for commercial jOOQ
* configurable to generate jOOQ classes for various frequencies 
(generate every time before compilation, or only if classes are absent, or never generate - relies on the user to run task manually)
* handles text-substitution in jooq-codegen.xml - which means you can use environment variables for database password,
or use SBT-settings like "Compile / sourceManaged" directly in the xml to set your target directory.
* works for all types of sbt projects.

If you are on JDK 8 you should definitely use the plugin. However, the plugin only supports up to JDK 10.

So this seed project intends to be a workaround for jOOQ integration for 
**Play Framework** on **JDK11**. Note: *Play Framework does not officially support JDK 11 yet.* 

Of course, once you understand the code you can easily change it to work for other types of SBT projects. 

Turn this into a plugin? The author is not proficient enough in Scala nor in SBT 
to fork or contribute to the [sbt-jooq](https://github.com/kxbmap/sbt-jooq/) plugin so this would have to do for now.

### Caveats

<details>
  <summary>For Java 8</summary>
  <p>Works for all new-ish versions of jOOQ: 3.11, 3.12</p>
  <p>In theory it should also work for earlier-but-new-ish versions 
     (this would be versions after `org.jooq.util.GenerationTool` 
     was renamed to `org.jooq.codegen.GenerationTool`) but testing is required.</p>
</details>
<details>
  <summary>For Java 11+</summary>
  <p>Only works for jOOQ 3.12</p>
  <p>
    This is due to classloading issues (JDK9 removed JAXB. JAXB implementations can be pulled in from maven it cannot be found by classloader).
  </p>
  <p>*Requires knowledge about classloaders and SBT to solve this*</p>
  <p>
    In response to Java 9 JPMS, jOOQ 3.12 has removed the dependency on JAXB and substituted with a home-grown
    XML library instead. Hence no more classloader issues.
  </p>  
</details>

In summary, JDK8 can use both jOOQ 3.11 and 3.12. JDK11+ can only use jOOQ 3.12.  

### How it works

The magic of code generation is done by the class `org.jooq.codegen.GenerationTool`. 
How do we invoke this from SBT as a SBT task? 

Some terminologies: 

* *project* is our project - in this case a Play *project*.
* *proper-build* is a Scala program which builds our *project*. 
All top-level sbt files (i.e. `/build.sbt`) or Scala files in project folder (e.g. `/project/Common.scala` is part of this "program".
* *meta-build* is a Scala program that builds our *proper-build*. It resides in `/project` folder. 
As with the *proper-build*, all top level sbt files or Scala files in project folder are part of this build.
So `/project/plugins.sbt` or `/project/project/Common.scala` is part of this build.
* It is basically a recursion. 
* For full explanation, see https://www.scala-sbt.org/1.x/docs/Organizing-Build.html.

So all we have to do is to put the `jooq-codegen` library to the classpath of our *proper-build*
and we can call the class `org.jooq.codegen.GenerationTool` in our sbt  to generate classes.

To put `GenerationTool` in classpath of *proper-build*, we use the *meta-build* to add
the library dependency.