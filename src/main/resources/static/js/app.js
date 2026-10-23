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
    el.innerHTML = `<i class="bi ${icons[tipo] || icons.info}"></i><span>${mensagem}</span>`;
    container.appendChild(el);

    setTimeout(() => {
        el.classList.add('saindo');
        setTimeout(() => el.remove(), 350);
    }, duracao);
}

// ---------- Fetch com CSRF ----------
async function postJson(url, body) {
    const res = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-XSRF-TOKEN': getCsrf()
        },
        body: JSON.stringify(body)
    });
    return res.json();
}

async function deleteJson(url) {
    const res = await fetch(url, {
        method: 'DELETE',
        headers: { 'X-XSRF-TOKEN': getCsrf() }
    });
    return res.json();
}

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
        icon.className = oculto ? 'bi bi-eye-slash' : 'bi bi-eye';
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