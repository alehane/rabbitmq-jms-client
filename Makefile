MVN ?= mvn
MVN_FLAGS ?=

ifndef DEPS_DIR
ifneq ($(wildcard ../../UMBRELLA.md),)
DEPS_DIR = ..
else
DEPS_DIR = deps
endif
endif

export PATH := $(CURDIR):$(CURDIR)/scripts:$(PATH)

MVN_FLAGS += -Ddeps.dir="$(abspath $(DEPS_DIR))"

.PHONY: all deps tests clean distclean

all: deps
	$(MVN) $(MVN_FLAGS) compile

deps: $(DEPS_DIR)/rabbit
	@:

dist: clean
	$(MVN) $(MVN_FLAGS) -DskipTests=true -Dmaven.javadoc.failOnError=false package javadoc:javadoc

$(DEPS_DIR)/rabbit:
	git clone https://github.com/rabbitmq/rabbitmq-server.git $@
	$(MAKE) -C $@ fetch-deps DEPS_DIR="$(abspath $(DEPS_DIR))"

clean:
	$(MVN) $(MVN_FLAGS) clean

distclean: clean
	$(MAKE) -C $(DEPS_DIR)/rabbitmq_codegen clean

.PHONY: cluster-other-node

.PHONY: doc
doc: ## Generate PerfTest documentation
	@mvnw asciidoctor:process-asciidoc --no-transfer-progress
