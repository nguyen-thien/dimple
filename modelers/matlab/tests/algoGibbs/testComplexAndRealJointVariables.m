%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%   Copyright 2012 Analog Devices, Inc.
%
%   Licensed under the Apache License, Version 2.0 (the "License");
%   you may not use this file except in compliance with the License.
%   You may obtain a copy of the License at
%
%       http://www.apache.org/licenses/LICENSE-2.0
%
%   Unless required by applicable law or agreed to in writing, software
%   distributed under the License is distributed on an "AS IS" BASIS,
%   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
%   See the License for the specific language governing permissions and
%   limitations under the License.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function testComplexAndRealJointVariables()

debugPrint = false;
repeatable = true;

dtrace(debugPrint, '++testOperatorOverloadingGibbs');

test1(debugPrint, repeatable);
test2(debugPrint, repeatable);
test3(debugPrint, repeatable);

dtrace(debugPrint, '--testOperatorOverloadingGibbs');

end

function test1(debugPrint, repeatable)

fg = FactorGraph();
fg.Solver = 'Gibbs';

a = Complex();
b = Complex();
c = Complex();
a.Name = 'a';
b.Name = 'b';
c.Name = 'c';

fg.addFactor('ComplexSum', c, a, b);

a.Input = {FactorFunction('Normal',3,5), FactorFunction('Normal',-1,1)};
b.Input = {FactorFunction('Normal',4,1), FactorFunction('Normal',6,2)};

fg.Solver.setNumSamples(1000);
fg.Solver.saveAllSamples();
fg.Solver.saveAllScores();

if (repeatable)
    fg.Solver.setSeed(1);					% Make this repeatable
end

fg.solve();

cs = c.Solver.getAllSamples;

assertElementsAlmostEqual(mean(cs), [7,5], 'absolute', 0.05);


end



function test2(debugPrint, repeatable)

numSamples = 100;
scansPerSample = 1;
burnInScans = 0;

fg = FactorGraph();
fg.Solver = 'Gibbs';
fg.Solver.setNumSamples(numSamples);
fg.Solver.setScansPerSample(scansPerSample);
fg.Solver.setBurnInScans(burnInScans);

x = Complex();
y = Complex();
x.Input = {FactorFunction('Normal',0,10), FactorFunction('Normal',0,10)};
y.Input = {FactorFunction('Normal',0,10), FactorFunction('Normal',0,10)};

a = x + y;
b = x - y;
c = x * y;
d = x / y;
e = -x;
f = x';
g = exp(x);

if (repeatable)
    fg.Solver.setSeed(1);					% Make this repeatable
end
fg.Solver.saveAllSamples();
fg.solve();

as = a.Solver.getAllSamples();
bs = b.Solver.getAllSamples();
cs = c.Solver.getAllSamples();
ds = d.Solver.getAllSamples();
es = e.Solver.getAllSamples();
fs = f.Solver.getAllSamples();
gs = g.Solver.getAllSamples();
xs = x.Solver.getAllSamples();
ys = y.Solver.getAllSamples();

ac = pairsToComplex(as);
bc = pairsToComplex(bs);
cc = pairsToComplex(cs);
dc = pairsToComplex(ds);
ec = pairsToComplex(es);
fc = pairsToComplex(fs);
gc = pairsToComplex(gs);
xc = pairsToComplex(xs);
yc = pairsToComplex(ys);


assertElementsAlmostEqual(ac, xc + yc, 'absolute');
assertElementsAlmostEqual(bc, xc - yc, 'absolute');
assertElementsAlmostEqual(cc, xc .* yc, 'absolute');
assertElementsAlmostEqual(dc, xc ./ yc, 'absolute');
assertElementsAlmostEqual(ec, -xc, 'absolute');
assertElementsAlmostEqual(fc, (xc').', 'absolute');
assertElementsAlmostEqual(gc, exp(xc), 'absolute');

end


function c = pairsToComplex(s)
c = s(:,1) + 1i*s(:,2);
end



function test3(debugPrint, repeatable)

numSamples = 20;
scansPerSample = 1;
burnInScans = 0;

fg = FactorGraph();
fg.Solver = 'Gibbs';
fg.Solver.setNumSamples(numSamples);
fg.Solver.setScansPerSample(scansPerSample);
fg.Solver.setBurnInScans(burnInScans);

N = 4;
a = Complex();
b = Complex(1,N);
c = Complex(1,N);
bt = Complex(N,1);

d = a + 1+2i;           % Complex scalar and complex scalar constant
e = b + 1+2i;           % Complex vector and complex scalar constant
f = a - b;              % Complex scalar and complex vector
g = 1+2i + a;           % Complex scalar constant and complex scalar
h = 1+2i + b;           % Complex scalar constant and complex vector
i = b - a;              % Complex vector and complex scalar

j = a * b;              % Complex scalar times vector (non-pointwise operator)
k = b * a;              % Complex vector times scalar (non-pointwise operator)
l = a .* b;             % Complex scalar times vector (pointwise operator)
m = b .* a;             % Complex vector times scalar (pointwise operator)
n = b .* c;             % Complex vector times complex vector (pointwise operator)

o = b / a;              % Complex vector divided by scalar (non-pointwise operator)
p = a ./ b;             % Complex scalar divided by vector (pointwise operator)
q = b ./ a;             % Complex vector divided by scalar (pointwise operator)
r = b ./ c;             % Complex vector divided by complex vector (pointwise operator)

s = b';                 % Conjugate transpose
t = bt';                % Conjugate transpose


if (repeatable)
    fg.Solver.setSeed(1);					% Make this repeatable
end
fg.Solver.saveAllSamples();
fg.solve();

as = a.invokeSolverMethodWithReturnValue('getAllSamples');
bs = b.invokeSolverMethodWithReturnValue('getAllSamples');
bts = bt.invokeSolverMethodWithReturnValue('getAllSamples');
cs = c.invokeSolverMethodWithReturnValue('getAllSamples');
ds = d.invokeSolverMethodWithReturnValue('getAllSamples');
es = e.invokeSolverMethodWithReturnValue('getAllSamples');
fs = f.invokeSolverMethodWithReturnValue('getAllSamples');
gs = g.invokeSolverMethodWithReturnValue('getAllSamples');
hs = h.invokeSolverMethodWithReturnValue('getAllSamples');
is = i.invokeSolverMethodWithReturnValue('getAllSamples');
js = j.invokeSolverMethodWithReturnValue('getAllSamples');
ks = k.invokeSolverMethodWithReturnValue('getAllSamples');
ls = l.invokeSolverMethodWithReturnValue('getAllSamples');
ms = m.invokeSolverMethodWithReturnValue('getAllSamples');
ns = n.invokeSolverMethodWithReturnValue('getAllSamples');
os = o.invokeSolverMethodWithReturnValue('getAllSamples');
ps = p.invokeSolverMethodWithReturnValue('getAllSamples');
qs = q.invokeSolverMethodWithReturnValue('getAllSamples');
rs = r.invokeSolverMethodWithReturnValue('getAllSamples');
ss = s.invokeSolverMethodWithReturnValue('getAllSamples');
ts = t.invokeSolverMethodWithReturnValue('getAllSamples');

ac = repmat(pairsToComplex(as), 1, N);
bc = cell2mat(cellfun(@(x)pairsToComplex(x), bs, 'UniformOutput', false));
btc = cell2mat(cellfun(@(x)pairsToComplex(x), bts, 'UniformOutput', false).').';
cc = cell2mat(cellfun(@(x)pairsToComplex(x), cs, 'UniformOutput', false));
dc = repmat(pairsToComplex(ds), 1, N);
ec = cell2mat(cellfun(@(x)pairsToComplex(x), es, 'UniformOutput', false));
fc = cell2mat(cellfun(@(x)pairsToComplex(x), fs, 'UniformOutput', false));
gc = repmat(pairsToComplex(gs), 1, N);
hc = cell2mat(cellfun(@(x)pairsToComplex(x), hs, 'UniformOutput', false));
ic = cell2mat(cellfun(@(x)pairsToComplex(x), is, 'UniformOutput', false));
jc = cell2mat(cellfun(@(x)pairsToComplex(x), js, 'UniformOutput', false));
kc = cell2mat(cellfun(@(x)pairsToComplex(x), ks, 'UniformOutput', false));
lc = cell2mat(cellfun(@(x)pairsToComplex(x), ls, 'UniformOutput', false));
mc = cell2mat(cellfun(@(x)pairsToComplex(x), ms, 'UniformOutput', false));
nc = cell2mat(cellfun(@(x)pairsToComplex(x), ns, 'UniformOutput', false));
oc = cell2mat(cellfun(@(x)pairsToComplex(x), os, 'UniformOutput', false));
pc = cell2mat(cellfun(@(x)pairsToComplex(x), ps, 'UniformOutput', false));
qc = cell2mat(cellfun(@(x)pairsToComplex(x), qs, 'UniformOutput', false));
rc = cell2mat(cellfun(@(x)pairsToComplex(x), rs, 'UniformOutput', false));
sc = cell2mat(cellfun(@(x)pairsToComplex(x), ss, 'UniformOutput', false).').';
tc = cell2mat(cellfun(@(x)pairsToComplex(x), ts, 'UniformOutput', false));

% Compare results
assertElementsAlmostEqual(dc, ac + 1+2i, 'absolute');            
assertElementsAlmostEqual(ec, bc + 1+2i, 'absolute');            
assertElementsAlmostEqual(fc, ac - bc, 'absolute');            
assertElementsAlmostEqual(gc, 1+2i + ac, 'absolute');            
assertElementsAlmostEqual(hc, 1+2i + bc, 'absolute');            
assertElementsAlmostEqual(ic, bc - ac, 'absolute');            
assertElementsAlmostEqual(jc, ac .* bc, 'absolute');            
assertElementsAlmostEqual(kc, bc .* ac, 'absolute');            
assertElementsAlmostEqual(lc, ac .* bc, 'absolute');            
assertElementsAlmostEqual(mc, bc .* ac, 'absolute');            
assertElementsAlmostEqual(nc, bc .* cc, 'absolute');            
assertElementsAlmostEqual(oc, bc ./ ac, 'absolute');            
assertElementsAlmostEqual(pc, ac ./ bc, 'absolute');            
assertElementsAlmostEqual(qc, bc ./ ac, 'absolute');            
assertElementsAlmostEqual(rc, bc ./ cc, 'absolute');            
assertElementsAlmostEqual(sc, bc', 'absolute');            
assertElementsAlmostEqual(tc, btc', 'absolute');         

end
