import copy, unittest
from contract_eval import cases, evaluate

class ChecksTest(unittest.TestCase):
    def fixture(self):
        c=cases()[0]
        headings=['Scope','Deliverables','Payment Terms','GST','IP Ownership','Confidentiality','Termination','Governing Law','Timeline']
        facts=' | '.join(str(v) for k,v in c.items() if k!='case_id')
        return c,dict(title=c['clientName'],executiveSummary='Review draft only',sections=[dict(heading=h,content=facts) for h in headings],reviewNotes=['Human review required'])

    def test_complete_draft_passes(self):
        c,d=self.fixture();self.assertTrue(all(evaluate(c,d).values()))

    def test_missing_clause_fails(self):
        c,d=self.fixture();d['sections'].pop();self.assertFalse(evaluate(c,d)['clauses'])

    def test_extra_money_and_date_fail(self):
        c,d=self.fixture();d['sections'][0]['content']+=' INR 99999999 due 2029-12-31';self.assertFalse(evaluate(c,d)['no_extra_money_dates'])

    def test_currency_formatting_is_not_an_extra_amount(self):
        c,d=self.fixture();d['sections'][0]['content']+=' INR 10,000';self.assertTrue(evaluate(c,d)['no_extra_money_dates'])

    def test_bad_schema_fails(self):
        c,d=self.fixture();d['sections'][0]['content']=None;self.assertFalse(evaluate(c,d)['schema'])

    def test_missing_client_is_detected(self):
        c,d=self.fixture();d['title']='Draft';
        for s in d['sections']:s['content']=s['content'].replace(c['clientName'],'Unknown client')
        self.assertFalse(evaluate(c,d)['supplied_facts'])

if __name__=='__main__':unittest.main()
