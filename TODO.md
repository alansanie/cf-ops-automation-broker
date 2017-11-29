
# Next cloudflare

- prevent secrets in exceptions from being exposed to end-users: catch exceptions
- (configure editor plugins to remind use of shortcuts)

- implement cf-ops-automation unit tests 

- cut 0.1 release & publish on github
- automate deployment in paas-template
    - automate ASG to git server 

- upgrade cf-ops-automation TF version to 0.11.0

- integration tests for jgit & related fixes 
    - implement caching of local repo: try a pull instead of a clone. Alternatives:
       - populate within the context a key to be persisted by OSB client (operation): the local clone path for a given request id. Enables caching across get/poll/update/delete
       - set up a cache queue shared by the different threads: each thread pop a clone in the queue. 
 
- cf-ops-automation pipeline: 
   - add a retry step on terraform-apply
   - add a serial statement to avoid concurrent triggers http://concourse.ci/configuring-jobs.html
   - secrets-<%=depls %> have an additional shared/secrets in path
   - remove duplicated paas-templates-full + add necessary paths to paas-template-<%=depls %>

- Application & Integration test:
   - support/test catalog injected from env var

- refine update-service status message to pass up the tf output status message ?

- harden cloudflare input validation: 
  - reject unsupported/unknown arbitrary params ? => Wait for JSON schema support

- simplify delete: TerraformRepository.deleteById()

- handle spaceid and orgguid being provided in OSB context (to plan for query params depreciation)


- refine status code in delete: return 410 GONE if service instance missing



- add stronger input validation to terraform module.name and outputs (from hashicorp hcl specs) to detect more issues up front if OSB-injected ids are HCL unfriendly   


Refine Repository impl
- terraform repository: 
   - test exception handling in streams.filter. Documentation is unclear about runtime exceptions 
   propagation in map() functions. Would they be caught by collectors or propagated ? 
- using spring data
    - Spring injection of Validation: constructor injection.
    - import spring data commons
    - SpringData import
    - SpringData file impl https://github.com/spring-projects/spring-data-keyvalue ?
        - https://github.com/spring-projects/spring-data-keyvalue/blob/master/src/main/asciidoc/key-value-repositories.adoc
        - https://github.com/hazelcast/spring-data-hazelcast/blob/master/src/main/java/org/springframework/data/hazelcast/HazelcastKeyValueAdapter.java
        - https://github.com/spring-projects/spring-data-keyvalue-examples/blob/master/retwisj/src/main/java/org/springframework/data/redis/samples/retwisj/redis/RetwisRepository.java
        - https://paulcwarren.github.io/spring-content/refs/release/fs-index.html
    - extract terraform state loading into a repository when needed to support a different backend than file (credhub, S3)




# Next core framework


- context key: encapsulate with methods + as immutable object ?
- update + bind/unbind request in context key
 
 
---------------
# Needs discussions


core framework:
- exception handling in processor in delete, getlastoperationstatus in ProcessorChainServiceInstanceService + ProcessorChain:
   default behavior to propagate exception upstream seems a good approach matching our needs: 
   - in delete: platform will return the delete failure to end-users which should retry
   - in get last create operation: platform will retry.
   
  - avoid internal exceptions to be user facing:
    Server error, status code: 502, error code: 10001, message: Service broker error: org.eclipse.jgit.api.errors.TransportException git repo url


- typing of exceptions throw by processors: RuntimeException ?
- spaces vs tabs indentation reported by intellij
- context key: encapsulate + as immutable object ?
- upgrade to mockito 2 https://github.com/mockito/mockito/wiki/What%27s-new-in-Mockito-2 

