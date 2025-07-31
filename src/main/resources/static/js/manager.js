document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const domainSelect = document.getElementById('domain-select');
    const pkInputSub = document.getElementById('pk-input-sub');
    const fetchBtn = document.getElementById('fetch-btn');
    const embedBtn = document.getElementById('embed-btn');
    const updateBtn = document.getElementById('update-btn');
    const deleteBtn = document.getElementById('delete-btn');
    const searchFilterInput = document.getElementById('search-filter-input');
    const tableHead = document.getElementById('data-table-head');
    const tableBody = document.getElementById('data-table-body');
    const paginationNav = document.getElementById('pagination-nav');
    const loadingIndicator = document.getElementById('loading-indicator');

    const dataModal = new bootstrap.Modal(document.getElementById('data-modal'));
    const modalLabel = document.getElementById('modal-label');
    const modalForm = document.getElementById('data-form');
    const modalSaveBtn = document.getElementById('modal-save-btn');

    let currentPage = 0;
    let currentDomain = 'place';
    let currentMainPk = null;

    // --- Configuration ---
    const domainConfigs = {
        'place': { columns: ['ID', 'Name', 'Address', 'Description', 'Created At', 'Updated At', 'Embedded At'], apiName: 'place' },
        'restaurant': { columns: ['ID', 'Name', 'Address', 'Category', 'Created At', 'Updated At', 'Embedded At'], apiName: 'restaurant' },
        'accom': { columns: ['ID', 'Name', 'Address', 'Type', 'Created At', 'Updated At', 'Embedded At'], apiName: 'accom' },
        'place_review': { columns: ['ID', 'Place ID', 'Rating', 'Content', 'Created At', 'Updated At', 'Embedded At'], apiName: 'place_review', isSub: true },
        'restaurant_review': { columns: ['ID', 'Restaurant ID', 'Rating', 'Content', 'Created At', 'Updated At', 'Embedded At'], apiName: 'restaurant_review', isSub: true },
        'accom_review': { columns: ['ID', 'Accom ID', 'Rating', 'Content', 'Created At', 'Updated At', 'Embedded At'], apiName: 'accom_review', isSub: true },
        'travel_style': { columns: ['ID', 'Style Type', 'Question', 'Answer', 'Created At', 'Updated At', 'Embedded At'], apiName: 'travel_style', isSub: true },
    };

    // --- Functions ---

    const showLoading = (show) => {
        loadingIndicator.style.display = show ? 'block' : 'none';
    };

    const formatDateTime = (dateTimeString) => {
        if (!dateTimeString) return '';
        try {
            const dt = new Date(dateTimeString);
            if (isNaN(dt.getTime())) return dateTimeString; // Invalid date

            const date = dt.getFullYear() + '/' + 
                         ('0' + (dt.getMonth() + 1)).slice(-2) + '/' + 
                         ('0' + dt.getDate()).slice(-2);
            const time = ('0' + dt.getHours()).slice(-2) + ':' + 
                         ('0' + dt.getMinutes()).slice(-2) + ':' + 
                         ('0' + dt.getSeconds()).slice(-2);
            return `${date}<br>${time}`;
        } catch (e) {
            return dateTimeString; // Return original string if parsing fails
        }
    };

    const fetchData = async (page = 0) => {
        currentDomain = domainSelect.value;
        const config = domainConfigs[currentDomain];
        const isSub = config.isSub;
        currentMainPk = pkInputSub.value;

        let url;
        if (isSub) {
            if (!currentMainPk) {
                alert('메인 도메인 PK를 입력하세요.');
                return;
            }
            url = `/manager/sub/${config.apiName}/${currentMainPk}?page=${page}&size=100`;
        } else {
            url = `/manager/main/${config.apiName}?page=${page}&size=100`;
        }

        showLoading(true);
        try {
            const response = await fetch(url);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const data = await response.json();
            renderTable(data.content, config.columns);
            renderPagination(data);
        } catch (error) {
            console.error('Fetch error:', error);
            alert('데이터를 불러오는 데 실패했습니다.');
            tableBody.innerHTML = `<tr><td colspan="${config.columns.length + 1}" class="text-center">데이터 로딩 실패</td></tr>`;
        } finally {
            showLoading(false);
        }
    };

    const renderTable = (data, columns) => {
        // Render Header
        tableHead.innerHTML = `
            <tr>
                <th scope="col" class="text-center"><input type="checkbox" id="select-all-checkbox"></th>
                ${columns.map(col => `<th scope="col">${col}</th>`).join('')}
            </tr>`;
        document.getElementById('select-all-checkbox').addEventListener('change', (e) => {
            tableBody.querySelectorAll('input[type="checkbox"]').forEach(checkbox => checkbox.checked = e.target.checked);
        });

        // Render Body
        if (!data || data.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="${columns.length + 1}" class="text-center">데이터가 없습니다.</td></tr>`;
            return;
        }

        tableBody.innerHTML = data.map(item => {
            const rowData = columns.map(col => {
                const key = col.toLowerCase().replace(/\s+/g, '_'); // e.g., 'Created At' -> 'created_at'
                let value = item[key] || '';

                // Handle camelCase keys from backend (e.g., createdAt)
                if (!value) {
                    const camelCaseKey = key.replace(/_([a-z])/g, g => g[1].toUpperCase());
                    value = item[camelCaseKey] || '';
                }

                // Format date-time columns
                if (['created_at', 'updated_at', 'embedded_at'].includes(key)) {
                    value = formatDateTime(value);
                }

                // Format date-time columns
                if (['created_at', 'updated_at', 'embedded_at'].includes(key)) {
                    value = formatDateTime(value);
                }

                return `<td>${value}</td>`;
            }).join('');
            return `
                <tr>
                    <td class="text-center"><input type="checkbox" class="form-check-input" data-pk="${item.id}"></td>
                    ${rowData}
                </tr>`;
        }).join('');
    };

    const renderPagination = (pageData) => {
        if (!pageData || pageData.totalPages <= 1) {
            paginationNav.innerHTML = '';
            return;
        }

        const { number, totalPages, first, last } = pageData;
        let paginationHtml = '<ul class="pagination justify-content-center">';

        // Previous button
        paginationHtml += `<li class="page-item ${first ? 'disabled' : ''}"><a class="page-link" href="#" data-page="${number - 1}">이전</a></li>`;

        // Page numbers
        for (let i = 0; i < totalPages; i++) {
            paginationHtml += `<li class="page-item ${i === number ? 'active' : ''}"><a class="page-link" href="#" data-page="${i}">${i + 1}</a></li>`;
        }

        // Next button
        paginationHtml += `<li class="page-item ${last ? 'disabled' : ''}"><a class="page-link" href="#" data-page="${number + 1}">다음</a></li>`;

        paginationHtml += '</ul>';
        paginationNav.innerHTML = paginationHtml;
    };

    const handleBatchOperation = async (operation) => {
        const selectedPks = Array.from(tableBody.querySelectorAll('input[type="checkbox"]:checked')).map(cb => cb.dataset.pk);
        if (selectedPks.length === 0) {
            alert('항목을 선택하세요.');
            return;
        }

        const domain = domainSelect.value;
        const url = `/manager/${domain}${selectedPks.length > 1 ? '/batch' : ''}`;
        const method = operation === 'embed' ? 'POST' : (operation === 'update' ? 'PATCH' : 'DELETE');

        const params = new URLSearchParams();
        selectedPks.forEach(pk => params.append('pks', pk));

        showLoading(true);
        try {
            const response = await fetch(`${url}?${params.toString()}`, { method });
            if (!response.ok) throw new Error('작업 실패');
            alert('작업 성공!');
            fetchData(currentPage);
        } catch (error) {
            console.error('Batch operation error:', error);
            alert(error.message);
        } finally {
            showLoading(false);
        }
    };

    // --- Event Listeners ---
    domainSelect.addEventListener('change', () => {
        const config = domainConfigs[domainSelect.value];
        pkInputSub.style.display = config.isSub ? 'block' : 'none';
        tableHead.innerHTML = '';
        tableBody.innerHTML = '';
        paginationNav.innerHTML = '';
    });

    fetchBtn.addEventListener('click', () => fetchData(0));

    paginationNav.addEventListener('click', (e) => {
        if (e.target.tagName === 'A') {
            e.preventDefault();
            const page = parseInt(e.target.dataset.page, 10);
            if (!isNaN(page)) {
                currentPage = page;
                fetchData(page);
            }
        }
    });

    embedBtn.addEventListener('click', () => handleBatchOperation('embed'));
    updateBtn.addEventListener('click', () => handleBatchOperation('update'));
    deleteBtn.addEventListener('click', () => {
        if (confirm('정말로 선택한 항목을 삭제하시겠습니까?')) {
            handleBatchOperation('delete');
        }
    });

    searchFilterInput.addEventListener('keyup', () => {
        const filterText = searchFilterInput.value.toLowerCase();
        tableBody.querySelectorAll('tr').forEach(row => {
            row.style.display = row.textContent.toLowerCase().includes(filterText) ? '' : 'none';
        });
    });

    // Initial Load
    fetchData();
});
