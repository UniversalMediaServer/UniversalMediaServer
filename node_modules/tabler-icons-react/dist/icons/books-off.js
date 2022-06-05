import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function BooksOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-books-off",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 9v10a1 1 0 0 1 -1 1h-2a1 1 0 0 1 -1 -1v-14"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 4a1 1 0 0 1 1 1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 5a1 1 0 0 1 1 -1h2a1 1 0 0 1 1 1v4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M13 13v6a1 1 0 0 1 -1 1h-2a1 1 0 0 1 -1 -1v-10"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M5 8h3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M9 16h4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14.254 10.244l-1.218 -4.424a1.02 1.02 0 0 1 .634 -1.219l.133 -.041l2.184 -.53c.562 -.135 1.133 .19 1.282 .732l3.236 11.75"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19.585 19.589l-1.572 .38c-.562 .136 -1.133 -.19 -1.282 -.731l-.952 -3.458"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 9l4 -1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19.207 15.199l.716 -.18"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { BooksOff as default };
