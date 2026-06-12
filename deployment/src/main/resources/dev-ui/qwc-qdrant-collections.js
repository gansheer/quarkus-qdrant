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

export class QwcQdrantCollections extends QwcHotReloadElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: block;
            padding: 16px;
        }
        .toolbar {
            display: flex;
            justify-content: space-between;
            align-items: flex-end;
            margin-bottom: 16px;
        }
        .create-form {
            display: flex;
            gap: 12px;
            align-items: flex-end;
            flex-wrap: wrap;
        }
        .toolbar-actions {
            display: flex;
            gap: 4px;
            align-items: center;
        }
    `;

    static properties = {
        _collections: {state: true},
        _loading: {state: true},
        _newName: {state: true},
        _newVectorSize: {state: true},
        _newDistance: {state: true},
        _isWatching: {state: true}
    };

    constructor() {
        super();
        this._collections = [];
        this._loading = true;
        this._newName = '';
        this._newVectorSize = 384;
        this._newDistance = 'Cosine';
        this._distances = [
            {label: 'Cosine', value: 'Cosine'},
            {label: 'Euclid', value: 'Euclid'},
            {label: 'Dot', value: 'Dot'},
            {label: 'Manhattan', value: 'Manhattan'}
        ];
        this._isWatching = false;
        this._watchId = null;
    }

    disconnectedCallback() {
        if (this._isWatching) this._unwatch();
        super.disconnectedCallback();
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
            <div class="toolbar">
                <div class="create-form">
                    <vaadin-text-field
                        label="Collection name"
                        .value="${this._newName}"
                        @value-changed="${e => this._newName = e.detail.value}">
                    </vaadin-text-field>
                    <vaadin-integer-field
                        label="Vector size"
                        .value="${this._newVectorSize}"
                        min="1"
                        @value-changed="${e => this._newVectorSize = parseInt(e.detail.value)}">
                    </vaadin-integer-field>
                    <vaadin-select
                        label="Distance"
                        .items="${this._distances}"
                        .value="${this._newDistance}"
                        @value-changed="${e => this._newDistance = e.detail.value}">
                    </vaadin-select>
                    <vaadin-button theme="primary"
                        @click="${this._create}"
                        ?disabled="${!this._newName || this._newName.trim().length === 0 || !this._newVectorSize || this._newVectorSize < 1}">
                        <vaadin-icon icon="font-awesome-solid:plus"></vaadin-icon>
                        Create
                    </vaadin-button>
                </div>
                <div class="toolbar-actions">
                    <vaadin-button theme="icon tertiary" title="Refresh" @click="${() => this.hotReload()}">
                        <vaadin-icon icon="font-awesome-solid:arrows-rotate"></vaadin-icon>
                    </vaadin-button>
                    <vaadin-button theme="icon tertiary" title="${this._isWatching ? 'Stop watching' : 'Start watching'}"
                        @click="${this._isWatching ? this._unwatch : this._watch}">
                        <vaadin-icon icon="font-awesome-solid:${this._isWatching ? 'eye' : 'eye-slash'}"></vaadin-icon>
                    </vaadin-button>
                </div>
            </div>

            <vaadin-grid .items="${this._collections}" theme="row-stripes">
                <vaadin-grid-column header="Name" path="name"></vaadin-grid-column>
                <vaadin-grid-column header="Status" path="status" auto-width></vaadin-grid-column>
                <vaadin-grid-column header="Points" path="pointsCount" auto-width></vaadin-grid-column>
                <vaadin-grid-column header="Vector Size" path="vectorSize" auto-width></vaadin-grid-column>
                <vaadin-grid-column header="Distance" path="distance" auto-width></vaadin-grid-column>
                <vaadin-grid-column header="Indexed Vectors" path="indexedVectorsCount" auto-width></vaadin-grid-column>
                <vaadin-grid-column header="Actions" auto-width
                    .renderer="${(root, _column, rowData) => {
                        root.innerHTML = '';
                        const btn = document.createElement('vaadin-button');
                        btn.setAttribute('theme', 'error tertiary small');
                        btn.innerHTML = '<vaadin-icon icon="font-awesome-solid:trash"></vaadin-icon> Delete';
                        btn.addEventListener('click', () => this._delete(rowData.item.name));
                        root.appendChild(btn);
                    }}">
                </vaadin-grid-column>
            </vaadin-grid>
        `;
    }

    hotReload() {
        this._loading = true;
        this.jsonRpc.listCollections().then(response => {
            const result = response.result;
            if (Array.isArray(result)) {
                this._collections = result;
            } else {
                this._collections = [];
            }
            this._loading = false;
        }, (error) => {
            const detail = error?.error?.message || 'Unknown error';
            notifier.showErrorMessage(`Failed to list collections: ${detail}`, 'top-end');
            this._loading = false;
        });
    }

    _create() {
        const name = this._newName.trim();
        if (!name) return;

        this.jsonRpc.createCollection({
            name: name,
            vectorSize: this._newVectorSize,
            distance: this._newDistance
        }).then(response => {
            const result = response.result;
            if (result.error) {
                notifier.showErrorMessage(result.error, 'top-end');
            } else {
                notifier.showInfoMessage(result.message, 'top-end');
                this._newName = '';
                this.hotReload();
            }
        });
    }

    _watch() {
        this._isWatching = true;
        this._watchId = setInterval(() => {
            this.hotReload();
        }, 3000);
    }

    _unwatch() {
        this._isWatching = false;
        clearInterval(this._watchId);
        this._watchId = null;
    }

    _delete(name) {
        this.jsonRpc.deleteCollection({name: name}).then(response => {
            const result = response.result;
            if (result.error) {
                notifier.showErrorMessage(result.error, 'top-end');
            } else {
                notifier.showInfoMessage(result.message, 'top-end');
                this.hotReload();
            }
        });
    }
}

customElements.define('qwc-qdrant-collections', QwcQdrantCollections);
