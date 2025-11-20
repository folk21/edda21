Output a strict JSON array where each item has this shape:
{
  "source": "LLM",
  "subject": "<string>",
  "topic": "<string>",
  "difficulty": "<A1|A2|B1|B2|C1|C2>",
  "type": "<OPEN|MULTI_CHOICE>",
  "body": "<short question text>",
  "options": ["<opt1>", "<opt2>", "<opt3>", "<opt4>"],  // required only for MULTI_CHOICE; omit for OPEN
  "correct": "<short correct answer or the correct option text>",
  "explanation": "<short rationale; optional>"
}
Return only the JSON array, nothing else.
