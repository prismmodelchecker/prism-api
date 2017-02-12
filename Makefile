
# Need to link to a PRISM distribution
PRISM_DIR = ../prism

# For compilation, just need access to classes/jars in the PRISM distribution
PRISM_CLASSPATH = "$(PRISM_DIR)/classes:$(PRISM_DIR)/lib/*"

# This Makefile just builds all java files in src and puts the class files in classes

JAVA_FILES := $(shell cd src && find . -name '*.java')
CLASS_FILES = $(JAVA_FILES:%.java=classes/%.class)

default: all

all: init $(CLASS_FILES)

init:
	@mkdir -p classes

classes/%.class: src/%.java
	(javac -classpath $(PRISM_CLASSPATH) -d classes $<)

# Test execution

test:
	PRISM_DIR=$(PRISM_DIR) PRISM_MAINCLASS=demos.ModelCheckFromFiles bin/run

# Clean up

clean:
	@rm -f $(CLASS_FILES)

celan: clean
