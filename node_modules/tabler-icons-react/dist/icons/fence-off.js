import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function FenceOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-fence-off",
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
    d: "M12 12h-8v4h12m4 0v-4h-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M6 16v4h4v-4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M10 12v-2m0 -4l-2 -2m-2 2v6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M14 16v4h4v-2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M18 12v-6l-2 -2l-2 2v4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { FenceOff as default };
