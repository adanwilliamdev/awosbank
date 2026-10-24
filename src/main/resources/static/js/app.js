/* ============================================================
   AWOS Bank — helpers globais
   ============================================================ */

// ---------- CSRF ----------
function getCsrf() {
    const m = document.cookie.split('; ').find(r => r.startsWith('XSRF-TOKEN='));
    return m ? decodeURIComponent(m.split('=')[1]) : '';
}

// ---------- Toast ----------
function toast(mensagem, tipo = 'sucesso', duracao = 4000) {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const icons = { sucesso: 'bi-check-circle-fill', erro: 'bi-x-circle-fill', info: 'bi-info-circle-fill' };
    const el = document.createElement('div');
    el.className = `toast-msg ${tipo}`;
    // textContent (nunca innerHTML): a mensagem pode conter dados vindos do usuário/servidor
    const icone = document.createElement('i');
    icone.className = `bi ${icons[tipo] || icons.info}`;
    const texto = document.createElement('span');
    texto.textContent = mensagem == null ? '' : String(mensagem);
    el.append(icone, texto);
    container.appendChild(el);

    setTimeout(() => {
        el.classList.add('saindo');
        setTimeout(() => el.remove(), 350);
    }, duracao);
}

// ---------- Fetch com CSRF ----------
// Sempre devolve { ok, mensagem, ... }, mesmo quando o servidor responde 403/401/500 ou algo
// que não seja JSON (antes isso estourava e o toast aparecia vazio).
async function requisicao(url, method, body) {
    const headers = { 'X-XSRF-TOKEN': getCsrf(), 'Accept': 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    let res;
    try {
        res = await fetch(url, {
            method, headers, credentials: 'same-origin',
            body: body === undefined ? undefined : JSON.stringify(body)
        });
    } catch (e) {
        return { ok: false, mensagem: 'Sem conexão com o servidor. Tente novamente.' };
    }
    if (res.redirected && res.url.includes('/login')) {
        return { ok: false, mensagem: 'Sua sessão expirou. Entre novamente.' };
    }
    let data = null;
    try { data = await res.json(); } catch (e) { /* corpo vazio ou não-JSON */ }
    if (data && typeof data === 'object' && 'ok' in data) {
        if (!data.ok && !data.mensagem) data.mensagem = mensagemPorStatus(res.status);
        return data;
    }
    if (res.ok) return { ok: true, mensagem: 'Operação realizada.', dados: data };
    return { ok: false, mensagem: mensagemPorStatus(res.status) };
}

function mensagemPorStatus(status) {
    if (status === 401) return 'Sua sessão expirou. Entre novamente.';
    if (status === 403) return 'Requisição recusada por segurança. Recarregue a página e tente de novo.';
    if (status === 404) return 'Recurso não encontrado.';
    if (status >= 500) return 'Erro interno no servidor. Tente novamente em instantes.';
    return 'Não foi possível concluir a operação.';
}

const postJson   = (url, body) => requisicao(url, 'POST', body);
const deleteJson = (url)       => requisicao(url, 'DELETE');

// ---------- Saldo toggle ----------
function toggleSaldo(btn) {
    const el = document.getElementById('saldo');
    if (!el) return;
    const icon = btn.querySelector('i');
    el.style.opacity = '0';
    btn.classList.toggle('rotated');
    setTimeout(() => {
        const real = el.getAttribute('data-real');
        const oculto = el.getAttribute('data-oculto') === 'true';
        el.textContent = oculto ? real : '••••••••';
        el.setAttribute('data-oculto', !oculto);
        el.style.opacity = '1';
        icon.className = oculto ? 'bi bi-eye-slash-fill' : 'bi bi-eye-fill';
    }, 180);
}

// ---------- Formatação moeda ----------
function formatBRL(v) {
    return 'R$ ' + Number(v).toLocaleString('pt-BR', {
        minimumFractionDigits: 2, maximumFractionDigits: 2
    });
}

// ---------- Modal Comprovante ----------
async function abrirComprovante(codigo) {
    try {
        const res = await fetch('/api/comprovante/' + codigo);
        const data = await res.json();
        if (!data.ok) { toast(data.mensagem || 'Erro', 'erro'); return; }

        document.getElementById('cmp-codigo').textContent = data.codigo;
        document.getElementById('cmp-tipo').textContent = data.tipo;
        document.getElementById('cmp-valor').textContent = formatBRL(data.valor);
        document.getElementById('cmp-data').textContent =
            new Date(data.dataHora).toLocaleString('pt-BR');
        document.getElementById('cmp-origem').textContent = data.origem || '—';
        document.getElementById('cmp-destino').textContent = data.destino || '—';
        document.getElementById('cmp-imprimir').onclick =
            () => window.open('/comprovante/' + codigo, '_blank');

        new bootstrap.Modal(document.getElementById('modalComprovante')).show();
    } catch (e) {
        toast('Erro ao carregar comprovante', 'erro');
    }
}

// ---------- Auto-show de toasts em flash attributes ----------
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-flash]').forEach(el => {
        toast(el.getAttribute('data-flash'), el.getAttribute('data-flash-tipo') || 'sucesso');
        el.remove();
    });
});

// ==========================================
// MÁSCARAS DE INPUT (NOVAS FUNCIONALIDADES)
// ==========================================

function aplicarMascaraMoeda(input) {
    let value = input.value.replace(/\D/g, '');
    value = (Number(value) / 100).toFixed(2) + '';
    value = value.replace(".", ",");
    value = value.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
    input.value = 'R$ ' + value;
}

function extrairValorMoeda(id) {
    const el = document.getElementById(id);
    if (!el) return 0;
    return parseFloat(el.value.replace('R$ ', '').replace(/\./g, '').replace(',', '.')) || 0;
}

function aplicarMascaraCpfCnpj(input) {
    let value = input.value.replace(/\D/g, '');
    if (value.length <= 11) {
        value = value.replace(/(\d{3})(\d)/, '$1.$2')
                     .replace(/(\d{3})(\d)/, '$1.$2')
                     .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
    } else {
        value = value.replace(/^(\d{2})(\d)/, '$1.$2')
                     .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
                     .replace(/\.(\d{3})(\d)/, '.$1/$2')
                     .replace(/(\d{4})(\d)/, '$1-$2');
    }
    input.value = value;
}

function aplicarMascaraTelefone(input) {
    let value = input.value.replace(/\D/g, '');
    if (value.length > 10) {
        value = value.replace(/^(\d{2})(\d{5})(\d{4}).*/, '($1) $2-$3');
    } else if (value.length > 5) {
        value = value.replace(/^(\d{2})(\d{4})(\d{0,4}).*/, '($1) $2-$3');
    } else if (value.length > 2) {
        value = value.replace(/^(\d{2})(\d{0,5})/, '($1) $2');
    } else {
        value = value.replace(/^(\d*)/, '($1');
    }
    input.value = value;
}