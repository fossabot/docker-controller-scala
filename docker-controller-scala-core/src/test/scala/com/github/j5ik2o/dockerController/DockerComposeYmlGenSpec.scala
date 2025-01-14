package com.github.j5ik2o.dockerController

import org.scalatest.freespec.AnyFreeSpec

import java.nio.file.{ Files, Paths }

class DockerComposeYmlGenSpec extends AnyFreeSpec {
  "DockerComposeYmlGenSpec" - {
    "generate" in {
      val tmpFile = Files.createTempFile(Paths.get("/tmp"), "docker-compose-", ".yml")

      DockerComposeYmlGen.generate(
        "docker-compose-2.yml.ftl",
        Map("nginxHostPort" -> Integer.valueOf(8080)),
        tmpFile.toFile
      )

    }
  }
}
