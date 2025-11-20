package com.edda21.llm.shared;

/** Constants for LLM operations. */
public final class LlmConstants {
  
  public static final class Prompts {
    public static final String SYSTEM = "prompts/questions/system.st";
    public static final String USER = "prompts/questions/user.st";
    public static final String OUTPUT_SPEC = "prompts/questions/output_spec.st";
    
    private Prompts() {}
  }
  
  public static final class Defaults {
    public static final String TOPIC = "general";
    public static final String DIFFICULTY = "B1";
    public static final String QUESTION_TYPE = "OPEN";
    public static final String LOCALE = "en";
    public static final String EXISTING_HASHES = "[]";
    
    private Defaults() {}
  }
  
  public static final class ContextKeys {
    public static final String SUBJECT = "subject";
    public static final String TOPIC = "topic";
    public static final String DIFFICULTY = "difficulty";
    public static final String QTYPE = "qtype";
    public static final String COUNT = "count";
    public static final String LOCALE = "locale";
    public static final String EXISTING_HASHES = "existing_hashes";
    
    private ContextKeys() {}
  }
  
  private LlmConstants() {}
}