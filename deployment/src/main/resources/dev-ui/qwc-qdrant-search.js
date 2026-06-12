import {QwcHotReloadElement, html, css} from 'qwc-hot-reload-element';
import {JsonRpc} from 'jsonrpc';
import {notifier} from 'notifier';
import '@vaadin/grid';
import '@vaadin/grid/vaadin-grid-column.js';
import '@vaadin/button';
import '@vaadin/text-field';
import '@vaadin/integer-field';
import '@vaadin/select';
import '@vaadin/icon';
import '@vaadin/progress-bar';

export class QwcQdrantSearch extends QwcHotReloadElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: block;
            padding: 16px;
        }
        .search-form {
            display: flex;
            gap: 12px;
            align-items: flex-end;
            margin-bottom: 16px;
            flex-wrap: wrap;
        }
        .search-form vaadin-text-field {
            flex: 1;
            min-width: 200px;
        }
        .payload-cell {
            font-family: monospace;
            font-size: 0.85em;
            white-space: pre-wrap;
            word-break: break-all;
        }
    `;

    static properties = {
        _collections: {state: true},
        _selectedCollection: {state: true},
        _vectorCsv: {state: true},
        _limit: {state: true},
        _results: {state: true},
        _loading: {state: true},
        _searching: {state: true}
    };

    constructor() {
        super();
        this._collections = [];
        this._selectedCollection = '';
        this._vectorCsv = '';
        this._limit = 10;
        this._results = [];
        this._loading = true;
        this._searching = false;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    render() {
        if (this._loading) {
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }

        return html`
            <div class="search-form">
                <vaadin-select
                    label="Collection"
                    .items="${this._collections.map(c => ({label: c.name, value: c.name}))}"
                    .value="${this._selectedCollection}"
                    @value-changed="${e => this._selectedCollection = e.detail.value}">
                </vaadin-select>
                <vaadin-text-field
                    label="Vector (comma-separated floats)"
                    placeholder="0.1, 0.2, 0.3, 0.4"
                    .value="${this._vectorCsv}"
                    @value-changed="${e => this._vectorCsv = e.detail.value}">
                </vaadin-text-field>
                <vaadin-integer-field
                    label="Limit"
                    .value="${this._limit}"
                    min="1"
                    max="100"
                    step-buttons-visible
                    @value-changed="${e => this._limit = parseInt(e.detail.value)}">
                </vaadin-integer-field>
                <vaadin-button theme="primary"
                    @click="${this._search}"
                    ?disabled="${!this._selectedCollection || !this._vectorCsv || this._searching}">
                    <vaadin-icon icon="font-awesome-solid:magnifying-glass"></vaadin-icon>
                    Search
                </vaadin-button>
            </div>

            ${this._searching
                ? html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`
                : html`
                    <vaadin-grid .items="${this._results}" theme="row-stripes">
                        <vaadin-grid-column header="ID" path="id" resizable></vaadin-grid-column>
                        <vaadin-grid-column header="Score" path="score" resizable width="100px"></vaadin-grid-column>
                        <vaadin-grid-column header="Payload" resizable
                            .renderer="${(root, _column, rowData) => {
                                root.innerHTML = '';
                                const span = document.createElement('span');
                                span.className = 'payload-cell';
                                span.textContent = rowData.item.payload
                                    ? JSON.stringify(rowData.item.payload, null, 2)
                                    : '';
                                root.appendChild(span);
                            }}">
                        </vaadin-grid-column>
                    </vaadin-grid>
                `
            }
        `;
    }

    hotReload() {
        this._loading = true;
        this.jsonRpc.listCollections().then(response => {
            const result = response.result;
            this._collections = Array.isArray(result) ? result : [];
            if (this._collections.length > 0 && !this._selectedCollection) {
                this._selectedCollection = this._collections[0].name;
            }
            this._loading = false;
        }, () => {
            notifier.showErrorMessage('Failed to list collections', 'top-end');
            this._loading = false;
        });
    }

    _search() {
        if (!this._selectedCollection || !this._vectorCsv) return;

        this._searching = true;
        this.jsonRpc.search({
            collection: this._selectedCollection,
            vectorCsv: this._vectorCsv.trim(),
            limit: this._limit || 10
        }).then(response => {
            const result = response.result;
            if (result.error) {
                notifier.showErrorMessage(result.error, 'top-end');
                this._results = [];
            } else {
                this._results = result.data || [];
            }
            this._searching = false;
        });
    }
}

customElements.define('qwc-qdrant-search', QwcQdrantSearch);
