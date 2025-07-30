document.addEventListener('DOMContentLoaded', () => {
    // Element Selectors
    const domainSelect = document.getElementById('domain-select');
    const pkInputForSub = document.getElementById('pk-input-sub');
    const fetchBtn = document.getElementById('fetch-btn');
    const addBtn = document.getElementById('add-btn');
    const editBtn = document.getElementById('edit-btn');
    const deleteBtn = document.getElementById('delete-btn');
    const dataTableBody = document.getElementById('data-table-body');
    const selectAllCheckbox = document.getElementById('select-all-checkbox');
    const searchFilterInput = document.getElementById('search-filter-input');

    const dataModal = new bootstrap.Modal(document.getElementById('data-modal'));
    const modalPkInput = document.getElementById('modal-pk');
    const modalSaveBtn = document.getElementById('modal-save-btn');

    // --- Event Listeners ---

    // 조회 버튼 클릭
    fetchBtn.addEventListener('click', () => {
        const domain = domainSelect.value;
        const isSubDomain = ['place_review', 'restaurant_review', 'accom_review', 'travel_style'].includes(domain);
        const mainPk = pkInputForSub.value;

        let url = `/manager/`;
        if (isSubDomain) {
            if (!mainPk) {
                alert('메인 도메인의 PK를 입력하세요.');
                return;
            }
            url += `sub/${domain}/${mainPk}`;
        } else {
            url += `main/${domain}`;
        }
        window.location.href = url; // 페이지 이동으로 데이터 조회 요청
    });

    // 도메인 변경 시 서브도메인 PK 입력 필드 표시/숨김
    domainSelect.addEventListener('change', () => {
        const selectedDomain = domainSelect.value;
        const isSubDomain = ['place_review', 'restaurant_review', 'accom_review', 'travel_style'].includes(selectedDomain);
        pkInputForSub.style.display = isSubDomain ? 'block' : 'none';
    });

    // 전체 선택 체크박스
    selectAllCheckbox.addEventListener('change', (e) => {
        dataTableBody.querySelectorAll('input[type="checkbox"]').forEach(checkbox => checkbox.checked = e.target.checked);
    });

    // 신규 추가 버튼 -> 모달 열기
    addBtn.addEventListener('click', () => {
        modalPkInput.value = '';
        dataModal.show();
    });

    // 선택 수정 버튼
    editBtn.addEventListener('click', () => {
        handleBatchOperation('patch');
    });

    // 선택 삭제 버튼
    deleteBtn.addEventListener('click', () => {
        handleBatchOperation('delete');
    });

    // 모달 저장 버튼 (신규 추가)
    modalSaveBtn.addEventListener('click', async () => {
        const domain = domainSelect.value;
        const pk = modalPkInput.value;
        if (!pk) {
            alert('PK를 입력하세요.');
            return;
        }

        try {
            const response = await fetch(`/manager/${domain}?pk=${pk}`, { method: 'POST' });
            if (!response.ok) throw new Error('저장 실패');
            alert('성공적으로 추가했습니다.');
            location.reload();
        } catch (error) {
            alert(error.message);
        }
    });

    // 테이블 내 실시간 필터링
    searchFilterInput.addEventListener('keyup', () => {
        const filterText = searchFilterInput.value.toLowerCase();
        dataTableBody.querySelectorAll('tr').forEach(row => {
            const rowText = row.textContent.toLowerCase();
            row.style.display = rowText.includes(filterText) ? '' : 'none';
        });
    });

    // --- Helper Functions ---

    // 선택된 PK 목록 가져오기
    function getSelectedPks() {
        return Array.from(dataTableBody.querySelectorAll('input[type="checkbox"]:checked'))
                    .map(cb => cb.dataset.pk);
    }

    // 수정/삭제 배치 작업 처리
    async function handleBatchOperation(operation) { // 'patch' or 'delete'
        const pks = getSelectedPks();
        if (pks.length === 0) {
            alert(`작업할 항목을 하나 이상 선택하세요.`);
            return;
        }
        if (operation === 'delete' && !confirm(`선택한 ${pks.length}개의 항목을 정말 삭제하시겠습니까?`)) {
            return;
        }

        const domain = document.getElementById('domain-select').value; // 현재 선택된 도메인
        const url = `/manager/${domain}${pks.length > 1 ? '/batch' : ''}?pks=${pks.join(',')}`;
        const method = operation === 'patch' ? 'PATCH' : 'DELETE';

        try {
            const response = await fetch(url, { method });
            if (!response.ok) throw new Error('작업 실패');
            alert('작업을 성공적으로 완료했습니다.');
            location.reload(); // 성공 시 페이지 새로고침
        } catch (error) {
            alert(error.message);
        }
    }
});
