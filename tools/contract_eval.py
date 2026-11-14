"""Compare versioned drafting prompts on synthetic cases. No legal-quality claim is made."""
import argparse, hashlib, html, json, os, re, time
from decimal import Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLAUSES = ['scope', 'deliverables', 'payment', 'gst', 'ip', 'confidentiality', 'termination', 'governing law', 'timeline']

def cases():
    result = []
    for i in range(30):
        service = ['web development', 'visual design', 'technical consulting'][i % 3]
        result.append(dict(case_id=f'case-{i+1:02}', clientName=f'Synthetic Client {i+1:02}',
            projectScope=f'Provide {service} for a business information portal with the explicitly listed deliverables.',
            deliverables=['Responsive website source code and operating guide', 'Brand identity files and design guide', 'Architecture review report and implementation roadmap'][i % 3],
            budgetGuidance=f'INR {10000+i*15000}', paymentTerms=['50% on approval; 50% on acceptance','25% on approval; 50% on milestone acceptance; 25% on completion'][i % 2],
            timeline=f'2027-01-{i%20+1:02} to 2027-03-{i%20+1:02}',
            gstStatus='GST included; rate to be confirmed' if i%2 else 'GST excluded; confirm applicability',
            jurisdiction='Ahmedabad, Gujarat' if i%2 else 'Mumbai, Maharashtra'))
    return result

def plain(value):
    return ' '.join(html.unescape(re.sub('<[^>]+>', ' ', value)).lower().split())

def evaluate(case, value):
    schema = isinstance(value, dict) and isinstance(value.get('title'), str) and isinstance(value.get('executiveSummary'), str) and isinstance(value.get('reviewNotes'), list) and isinstance(value.get('sections'), list)
    schema = bool(schema and value['sections'] and all(isinstance(s,dict) and isinstance(s.get('heading'),str) and isinstance(s.get('content'),str) and len(plain(s['content']).strip())>=15 for s in value['sections']))
    if not schema:
        return dict(schema=False, clauses=False, supplied_facts=False, no_extra_money_dates=False)
    headings = ' '.join(s['heading'] for s in value['sections']).lower()
    text = plain(value['title']+' '+value['executiveSummary']+' '+' '.join(s['content'] for s in value['sections']))
    inputs = ' '.join(str(v) for k,v in case.items() if k!='case_id').lower()
    money = re.findall(r'(?:inr|rs\.?|₹)\s*[\d,]+(?:\.\d+)?',text)
    dates = re.findall(r'\b\d{4}-\d{2}-\d{2}\b',text)
    money_pattern = r'(?:\binr\b|\brs\.?|₹)\s*(\d[\d,]*(?:\.\d+)?)'
    supplied_money = {Decimal(n.replace(',', '')) for n in re.findall(money_pattern, inputs)}
    output_money = {Decimal(n.replace(',', '')) for n in re.findall(money_pattern, text)}
    return dict(schema=schema,clauses=all(c in headings for c in CLAUSES),
        supplied_facts=all(case[f].lower() in text for f in ['clientName','budgetGuidance','paymentTerms','timeline','gstStatus','jurisdiction']),
        no_extra_money_dates=output_money.issubset(supplied_money) and all(x in inputs for x in dates))

def run(args):
    output=ROOT/'eval'; cache=output/'private'; cache.mkdir(parents=True,exist_ok=True)
    dataset=cases(); (output/'cases.json').write_text(json.dumps(dataset,indent=2),encoding='utf-8')
    if args.generate:
        from dotenv import load_dotenv
        from google import genai
        from google.genai import types
        if args.env_file: load_dotenv(args.env_file)
        key=os.getenv('GEMINI_API_KEY') or os.getenv('GOOGLE_API_KEY')
        if not key: raise RuntimeError('GEMINI_API_KEY is required; no requests made')
        client=genai.Client(api_key=key,http_options=types.HttpOptions(timeout=90000))
    rows=[]; last=0
    for version in ['contract_v1','contract_v2']:
        prompt=(ROOT/'backend/src/main/resources/prompts'/f'{version}.txt').read_text(encoding='utf-8')
        for case in dataset:
            digest=hashlib.sha256((args.model+prompt+json.dumps(case,sort_keys=True)).encode()).hexdigest()
            path=cache/f'{version}-{case["case_id"]}-{digest[:16]}.json'
            value=None; error=''
            if path.exists(): value=json.loads(path.read_text(encoding='utf-8'))
            elif args.generate:
                time.sleep(max(0,args.interval-(time.monotonic()-last))); last=time.monotonic()
                try:
                    response=client.models.generate_content(model=args.model,contents='PROJECT PARAMETERS:\n'+json.dumps(case),config=types.GenerateContentConfig(system_instruction=prompt,response_mime_type='application/json',temperature=0.2,max_output_tokens=4096))
                    value=json.loads(response.text)
                    path.write_text(json.dumps(value,indent=2),encoding='utf-8')
                except Exception as exc:
                    error=type(exc).__name__
                    print(version,case['case_id'],'request failed:',error,flush=True)
            checks=evaluate(case,value)
            rows.append(dict(version=version,case_id=case['case_id'],completed=value is not None,error=error,**checks))
            print(version,case['case_id'],checks,flush=True)
    lines=['# Synthetic contract-drafting evaluation','',f'Model: `{args.model}`. Thirty synthetic inputs per prompt version. One generation per case; cached responses are reused.','', '| Prompt | Completed | Schema | All clauses | Supplied facts | No extra detected amounts/dates | All checks |','| --- | --- | --- | --- | --- | --- | --- |']
    for version in ['contract_v1','contract_v2']:
        subset=[r for r in rows if r['version']==version]
        counts=[sum(r[k] for r in subset) for k in ['completed','schema','clauses','supplied_facts','no_extra_money_dates']]
        all_ok=sum(all(r[k] for k in ['completed','schema','clauses','supplied_facts','no_extra_money_dates']) for r in subset)
        lines.append('| '+version+' | '+' | '.join(str(n)+'/30' for n in counts+[all_ok])+' |')
    lines+=['','## Limits','','These are generated outputs on synthetic cases, not real client contracts or legal review. Schema checks are structural, clause checks use headings and non-empty content, and fact checks require literal preservation. Extra-amount/date detection uses patterns and does not detect arbitrary invented parties or all semantic errors. This does not establish legal adequacy. Missing or failed generations do not count as completed. Local fallback-template tests are separate and are never counted as model-generation results.','', '## Per-case checks','', '| Prompt | Case | Completed | Schema | Clauses | Facts | Amounts/dates |','| --- | --- | --- | --- | --- | --- | --- |']
    lines += ['| '+' | '.join(str(r[k]) for k in ['version','case_id','completed','schema','clauses','supplied_facts','no_extra_money_dates'])+' |' for r in rows]
    (output/'REPORT.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    (output/'checks.json').write_text(json.dumps(rows,indent=2),encoding='utf-8')
    print('Report:',output/'REPORT.md')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--generate',action='store_true');p.add_argument('--env-file');p.add_argument('--model',default='gemini-3.5-flash-lite');p.add_argument('--interval',type=float,default=6.2);run(p.parse_args())
