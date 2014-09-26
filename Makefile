JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	CurrencyValue.java \
	Lamport.class

default: classes

classes: $(CLASSES:.java=.class)

clean:
	find . -name \*.class | xargs $(RM) 
	$(RM) log*
