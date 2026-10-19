#!/bin/sh
set -eu

NAME=${1:?usage: make module NAME=example-billing}

case "$NAME" in
	'' | *[!A-Za-z0-9._-]*)
		echo "invalid module name: '$NAME' (letters, digits, . _ - only)" >&2
		exit 2
		;;
esac

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"
POM=pom.xml

[ -f "$POM" ] || { echo "cannot find $POM in $ROOT" >&2; exit 2; }
grep -q "<modules>" "$POM" || { echo "root pom has no <modules> block" >&2; exit 2; }
[ -d "$NAME" ] && { echo "folder '$NAME' already exists, aborting" >&2; exit 2; }

base=$(printf '%s' "$NAME" | sed 's/^example-//')
dots=$(printf '%s' "$base" | tr 'A-Z' 'a-z' | tr -c 'a-z0-9' '.' | tr -s '.' | sed 's/^\.//; s/\.$//')
PKG=com.example${dots:+.$dots}
PKG_PATH=$(printf '%s' "$PKG" | tr '.' '/')
MAIN=$(printf '%s' "$base" | sed 's/-/ /g' \
	| awk '{ for (i = 1; i <= NF; i++) $i = toupper(substr($i, 1, 1)) substr($i, 2) } 1' \
	| tr -d ' \n')
[ -n "$MAIN" ] || MAIN=App
MAIN_CLASS=${MAIN}Application

SRC_MAIN=src/main/java
SRC_TEST=src/test/java
mkdir -p "$NAME/$SRC_MAIN/$PKG_PATH" "$NAME/src/main/resources" "$NAME/$SRC_TEST/$PKG_PATH"

cat > "$NAME/pom.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.example</groupId>
    <artifactId>root</artifactId>
    <version>1.0-SNAPSHOT</version>
  </parent>

  <artifactId>$NAME</artifactId>
  <name>$NAME</name>

  <dependencies>
    <dependency>
      <groupId>com.example</groupId>
      <artifactId>shared-kernel</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webmvc-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
EOF

cat > "$NAME/$SRC_MAIN/$PKG_PATH/$MAIN_CLASS.java" <<EOF
package $PKG;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class $MAIN_CLASS {

    public static void main(String[] args) {
        SpringApplication.run($MAIN_CLASS.class, args);
    }
}
EOF

cat > "$NAME/$SRC_TEST/$PKG_PATH/${MAIN_CLASS}Tests.java" <<EOF
package $PKG;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ${MAIN_CLASS}Tests {

    @Test
    void contextLoads() {
    }
}
EOF

cat > "$NAME/src/main/resources/application.yml" <<EOF
spring:
  application:
    name: $NAME
  jpa:
    open-in-view: false

server:
  port: \${SERVER_PORT:8080}
EOF

if grep -q "<module>$NAME</module>" "$POM"; then
	echo "root pom already lists $NAME"
else
	awk -v mod="$NAME" '
		!injected && /^[ \t]*<\/modules>/ { print "    <module>" mod "</module>"; injected = 1 }
		{ print }
	' "$POM" > "$POM.new"
	grep -q "<module>$NAME</module>" "$POM.new" || { echo "failed to inject into $POM" >&2; rm -f "$POM.new"; exit 1; }
	mv "$POM.new" "$POM"
fi

echo "module '$NAME' generated:"
echo "  artifactId : $NAME"
echo "  package    : $PKG"
echo "  main class : $MAIN_CLASS"
echo ""
echo "next steps:"
echo "  make run NAME=$NAME"
echo "  ./mvnw -pl $NAME -am test"
echo ""
echo "in IDEA: Maven tool window -> Reload All Maven Projects"
