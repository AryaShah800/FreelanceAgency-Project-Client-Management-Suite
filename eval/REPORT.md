# Synthetic contract-drafting evaluation

Model: `gemini-3.5-flash-lite`. Thirty synthetic inputs per prompt version. One generation per case; cached responses are reused.

| Prompt | Completed | Schema | All clauses | Supplied facts | No extra detected amounts/dates | All checks |
| --- | --- | --- | --- | --- | --- | --- |
| contract_v1 | 30/30 | 30/30 | 0/30 | 0/30 | 30/30 | 0/30 |
| contract_v2 | 30/30 | 30/30 | 30/30 | 18/30 | 30/30 | 18/30 |

## Limits

These are generated outputs on synthetic cases, not real client contracts or legal review. Schema checks are structural, clause checks use headings and non-empty content, and fact checks require literal preservation. Extra-amount/date detection uses patterns and does not detect arbitrary invented parties or all semantic errors. This does not establish legal adequacy. Missing or failed generations do not count as completed. Local fallback-template tests are separate and are never counted as model-generation results.

## Per-case checks

| Prompt | Case | Completed | Schema | Clauses | Facts | Amounts/dates |
| --- | --- | --- | --- | --- | --- | --- |
| contract_v1 | case-01 | True | True | False | False | True |
| contract_v1 | case-02 | True | True | False | False | True |
| contract_v1 | case-03 | True | True | False | False | True |
| contract_v1 | case-04 | True | True | False | False | True |
| contract_v1 | case-05 | True | True | False | False | True |
| contract_v1 | case-06 | True | True | False | False | True |
| contract_v1 | case-07 | True | True | False | False | True |
| contract_v1 | case-08 | True | True | False | False | True |
| contract_v1 | case-09 | True | True | False | False | True |
| contract_v1 | case-10 | True | True | False | False | True |
| contract_v1 | case-11 | True | True | False | False | True |
| contract_v1 | case-12 | True | True | False | False | True |
| contract_v1 | case-13 | True | True | False | False | True |
| contract_v1 | case-14 | True | True | False | False | True |
| contract_v1 | case-15 | True | True | False | False | True |
| contract_v1 | case-16 | True | True | False | False | True |
| contract_v1 | case-17 | True | True | False | False | True |
| contract_v1 | case-18 | True | True | False | False | True |
| contract_v1 | case-19 | True | True | False | False | True |
| contract_v1 | case-20 | True | True | False | False | True |
| contract_v1 | case-21 | True | True | False | False | True |
| contract_v1 | case-22 | True | True | False | False | True |
| contract_v1 | case-23 | True | True | False | False | True |
| contract_v1 | case-24 | True | True | False | False | True |
| contract_v1 | case-25 | True | True | False | False | True |
| contract_v1 | case-26 | True | True | False | False | True |
| contract_v1 | case-27 | True | True | False | False | True |
| contract_v1 | case-28 | True | True | False | False | True |
| contract_v1 | case-29 | True | True | False | False | True |
| contract_v1 | case-30 | True | True | False | False | True |
| contract_v2 | case-01 | True | True | True | False | True |
| contract_v2 | case-02 | True | True | True | False | True |
| contract_v2 | case-03 | True | True | True | True | True |
| contract_v2 | case-04 | True | True | True | True | True |
| contract_v2 | case-05 | True | True | True | True | True |
| contract_v2 | case-06 | True | True | True | True | True |
| contract_v2 | case-07 | True | True | True | True | True |
| contract_v2 | case-08 | True | True | True | True | True |
| contract_v2 | case-09 | True | True | True | False | True |
| contract_v2 | case-10 | True | True | True | True | True |
| contract_v2 | case-11 | True | True | True | False | True |
| contract_v2 | case-12 | True | True | True | False | True |
| contract_v2 | case-13 | True | True | True | True | True |
| contract_v2 | case-14 | True | True | True | True | True |
| contract_v2 | case-15 | True | True | True | True | True |
| contract_v2 | case-16 | True | True | True | True | True |
| contract_v2 | case-17 | True | True | True | True | True |
| contract_v2 | case-18 | True | True | True | True | True |
| contract_v2 | case-19 | True | True | True | False | True |
| contract_v2 | case-20 | True | True | True | False | True |
| contract_v2 | case-21 | True | True | True | True | True |
| contract_v2 | case-22 | True | True | True | True | True |
| contract_v2 | case-23 | True | True | True | True | True |
| contract_v2 | case-24 | True | True | True | True | True |
| contract_v2 | case-25 | True | True | True | True | True |
| contract_v2 | case-26 | True | True | True | False | True |
| contract_v2 | case-27 | True | True | True | False | True |
| contract_v2 | case-28 | True | True | True | False | True |
| contract_v2 | case-29 | True | True | True | False | True |
| contract_v2 | case-30 | True | True | True | False | True |
