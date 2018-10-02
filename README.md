# cf-ops-automation-broker [![CI](https://circleci.com/gh/orange-cloudfoundry/cf-ops-automation-broker.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/orange-cloudfoundry/cf-ops-automation-broker)
On demand dedicated services through COA concourse pipeline engine

## Overview

COAB is a generic service broker which enables operators to provide on-demand dedicated services from available building blocks:
* terraform modules provisionning
   * [cloudfoundry resources](https://github.com/mevansam/terraform-provider-cf), such as cloudfoundry applications
   * [K8S resources](https://www.terraform.io/docs/providers/kubernetes/), e.g. through [K8S charts](https://github.com/mcuadros/terraform-provider-helm)
   * Saas resources such as [cloudflare](https://www.terraform.io/docs/providers/cloudflare/)
* existing bosh releases with service brokers offering shared service plans (e.g [cassandra-cf-service-boshrelease](https://github.com/orange-cloudfoundry/cassandra-cf-service-boshrelease/))

COAB leverages concourse-based pipelines to deploy and operate the dedicated resources, managed by the [cf-ops-automation (COA)](https://github.com/orange-cloudfoundry/cf-ops-automation) collaboration framework.

Here are some slides to provide more background to this project:

[![image](https://user-images.githubusercontent.com/4748380/46364595-85ead380-c676-11e8-947d-2c6b32516a43.png)](https://docs.google.com/presentation/d/1ChrpHQRxwdLzpt4m4sLe2PVJICqP0bCo9B5ffCPv57Q/edit?usp=sharing)

The following diagram illustrates COAB interactions
![Overview of COAB interactions](coab-overview.png)

1. A user requests a dedicated service instance (or binding) through its prefered platform (CF or K8S service catalog) which in turn propagate the request to COAB as an [OSB API call](https://github.com/openservicebrokerapi/servicebroker)
2. COAB requests an on-demand dedicated pipeline to the COA templating engine, by writing to Git repos
3. COA templating engine reads the git repos 
4. COA generates on-demand pipelines that deploys and operate the requested dedicate service resources
5. The dedicated concourse pipeline provisions resources supporting the dedicated resource (in form of a bosh deployment, a terraform module, or a CF app)
7. the dedicated concourse pipeline records the outcome of the dedicated service in git
8. the dedicated concourse pipeline (and its underlying tools such as bosh director) records the credentials necessary to consumme the decicated service instance/binding
9. COAB pulls the dedicated service provisionning completion status from git
10a. COAB delegates OSB API calls to nested service brokers, delegating credentials management to them
10b. alternatively COAB fetches dedicated service credentials generated by the pipeline from credhub
11. COAB returns service instance/bindings to user





## Status

This project is still in a beta state providing POC broker releases for CloudFlare and Cassandra use-cases above. Next step is to generalize the approach.

## Getting Started

Deploy the broker as a CF app:
* The CF manifest.yml file would be available in [orange-cloudfoundry/paas-templates](https://github.com/orange-cloudfoundry/paas-templates) repo
   * The documentation reference for supported and required environment variables is in the [integration tests properties](cf-ops-automation-bosh-broker/src/test/resources/application.properties). 
   * The broker does not require stateful data service to be bound. 

### Configuring the service broker catalog

Use `CATALOG_YML` environment variable to set catalog config in a YAML format. See [catalog.yml](cf-ops-automation-sample-broker/catalog.yml) for a sample YML file which corresponds to the [Catalog bean](https://github.com/spring-cloud/spring-cloud-open-service-broker/blob/v1.0.2.RELEASE/src/main/java/org/springframework/cloud/servicebroker/model/Catalog.java).

## Contributing

 ### Releasing

Prereqs: checkout the branch to release, and make sure it is up-to-date w.r.t. the github remote.
 
Releasing is made using [maven release plugin](http://maven.apache.org/maven-release/maven-release-plugin/) as follows :
 
 ```shell
 
 $ mvn release:prepare --batch-mode -Dtag={your git tag} -DreleaseVersion={release version to be set} -DdevelopmentVersion={next snapshot version to be set}
 
 # ex : mvn release:prepare --batch-mode -Dtag=v0.21.0 -DreleaseVersion=0.21.0 -DdevelopmentVersion=0.22.0-SNAPSHOT
 
 ```
 
 Circle CI build will proceed, and will trigger the execution of `mvn release:perform`, and upload artifacts to both github and https://bintray.com/elpaaso. For further details, see [release:prepare goals](http://maven.apache.org/maven-release/maven-release-plugin/prepare-mojo.html)

Following the release:
- edit the release notes in github
- clean up your local workspace using `mvn release:clean`

In case of issues, try:
* `mvn release:rollback` 
    * possibly revert the commits in git (`git reset --hard commitid`), 
* clean up the git tag `git tag -d vXX && git push --delete origin vXX`, 
* `mvn release:clean`
* fix the root cause and retry.
 
