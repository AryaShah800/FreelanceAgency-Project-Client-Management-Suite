# Versioned contract-drafting evaluation

The AI generator uses `backend/src/main/resources/prompts/contract_v2.txt`. The previous proposal prompt is preserved as `contract_v1.txt`. Responses identify `promptVersion`: `contract_v2` for validated generated drafts or `template_v2` for the local fallback. The fallback is never counted as a Gemini result.

Inputs now include optional GST status and jurisdiction. The service includes budget guidance and payment terms in the model request, checks required clause headings and literal commercial facts, and rejects incomplete or altered output. Missing fields remain explicit confirmation items. The local fallback escapes input and invents no budget or date defaults.

Run the objective evaluator using Python with `google-genai` and `python-dotenv` installed:

```powershell
python tools/contract_eval.py --generate --env-file /path/to/your/local.env
python tools/contract_eval.py
python -m unittest discover -s tools -p "test_*.py"
mvn -f backend/pom.xml test
```

The 30 synthetic cases span web, design and consulting services, different budgets/payment schedules, GST-included/excluded wording, and Gujarat/Maharashtra jurisdictions. `eval/REPORT.md` records measured v1/v2 outcomes. `eval/private/` caches generated text locally and is ignored. Reruns reuse cached responses and retry missing generations; a missing generation never counts as completed.

The evaluator checks JSON structure, clause headings with nonempty content, literal fact preservation, and unexpected currency amounts/ISO dates. It does not prove legal adequacy, detect all invented parties or semantic errors, or replace legal review. Requests have synthetic inputs, not real client data. The generation settings in the evaluation are temperature 0.2, 4096 output tokens; production defaults match, but deployment configuration may override token limits.

The original endpoint still returns the existing proposal-shaped DTO for frontend compatibility. It now drafts a services-agreement review document rather than claiming a signed, legally validated contract. Signing and audit workflows remain separate.
